{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE OverloadedStrings #-}

module Data.OSPDiff.Trace where

import Prelude

import Data.List (find)
import Control.Applicative (empty, (<|>))
import Control.Monad
import Data.Aeson
import Data.Aeson.Internal (JSONPath, iparse, formatError)
import Data.Aeson.Types (Parser, parse)
import Data.Aeson.Parser (decodeWith, eitherDecodeWith, json)
import Data.Text (Text, isSuffixOf)
import Data.Time (UTCTime)
import qualified Data.HashMap.Lazy as H (lookup, keys)
import qualified Data.ByteString.Lazy.Internal as BLI (ByteString)
import qualified Data.ByteString.Lazy as BS (pack, readFile, writeFile)


-- ADTs
data HTTP = Post | Get | Update | Delete deriving (Show, Eq)

data HTTPReq = HTTPReq
  { path   :: String
  , method :: HTTP
  , query  :: String
  } deriving (Show, Eq)

data DBReq = DBReq
  { stmt   :: String
  , params :: Value
  } deriving (Show, Eq)

data PythonReq = PythonReq
  { function :: String
  , args     :: String
  , kwargs   :: String
  } deriving (Show, Eq)

data (Show a, Eq a) => TraceInfo a = TraceInfo
  { project  :: String
  , service  :: String
  , start    :: UTCTime
  , stop     :: Maybe String
  , req      :: a
  } deriving (Show, Eq)

data Trace = Wsgi (TraceInfo HTTPReq) [Trace]
           | DB (TraceInfo DBReq) [Trace]
           | RPC (TraceInfo PythonReq) [Trace]
           | ComputeApi (TraceInfo PythonReq) [Trace]
           | NovaImage (TraceInfo PythonReq) [Trace]
           | NovaVirt (TraceInfo PythonReq) [Trace]
           | NeutronApi (TraceInfo PythonReq) [Trace]
           deriving Show


-- Utils
(.:+) :: (FromJSON a) => Value -> [Text] -> Parser a
(.:+) v = parseJSON <=< foldM ((maybe empty pure .) . lookupE) v
  where
    lookupE :: Value -> Text -> Maybe Value
    lookupE (Object v') key = H.lookup key v'
    lookupE _           _   = Nothing

(.:*-) :: (FromJSON a) => Object -> Text -> Parser a
(.:*-) o = parseJSON <=< ((maybe empty pure .) . lookupRE) o
  where
    lookupRE :: Object -> Text -> Maybe Value
    lookupRE o' suffix = do k <- find (isSuffixOf suffix) (H.keys o')
                            H.lookup k o'

(.:*-?) :: (FromJSON a) => Object -> Text -> Parser (Maybe a)
(.:*-?) o s = (<=<) (pure . Just) (.:*- s) o <|> pure Nothing

class (FromJSON a, Show a, Eq a) => ReqPath a where
  reqPath :: Value -> Parser a

parserTopTrace :: Value -> Parser [Trace]
parserTopTrace (Object o) = o .: "children"
parserTopTrace _          = empty

-- ReqPath instances
instance ReqPath HTTPReq where
  reqPath = flip (.:+) [ "meta.raw_payload.wsgi-start", "info", "request" ]

instance ReqPath DBReq where
  reqPath = flip (.:+) [ "meta.raw_payload.db-start", "info", "db" ]

instance ReqPath PythonReq where
  reqPath v =
    let rpc        = v .:+ [ "meta.raw_payload.rpc-start",         "info", "function" ]
        computeApi = v .:+ [ "meta.raw_payload.compute_api-start", "info", "function" ]
        novaImage  = v .:+ [ "meta.raw_payload.nova_image-start",  "info", "function" ]
        novaVirt   = v .:+ [ "meta.raw_payload.vif_driver-start",  "info", "function" ]
        neutronApi = v .:+ [ "meta.raw_payload.neutron_api-start", "info", "function" ]
    in rpc <|> computeApi <|> novaImage <|> novaVirt <|> neutronApi

-- FronJSON instances
instance FromJSON HTTP where
  parseJSON (String s) = case s of
    "POST"   -> pure Post
    "GET"    -> pure Get
    "UPDATE" -> pure Update
    "DELETE" -> pure Delete
    _        -> empty
  parseJSON _          = empty

instance FromJSON HTTPReq where
  parseJSON (Object o) = HTTPReq <$>
        o .: "path"
    <*> o .: "method"
    <*> o .: "query"
  parseJSON _          = empty

instance FromJSON DBReq where
  parseJSON (Object o) = DBReq <$>
        o .: "statement"
    <*> o .: "params"
  parseJSON _          = empty

instance FromJSON PythonReq where
  parseJSON (Object o) = PythonReq <$>
        o .: "name"
    <*> o .: "args"
    <*> o .: "kwargs"
  parseJSON _          = empty

instance (ReqPath a, FromJSON a) => FromJSON (TraceInfo a) where
  parseJSON v@(Object o) = TraceInfo <$>
        o .: "project"
    <*> o .: "service"
    <*> ((.: "timestamp") <=< (.:*-  "-start")) o
    <*> (maybe (pure Nothing) (.: "timestamp") <=< (.:*-? "-stop"))  o
    <*> reqPath v
  parseJSON _            = empty

instance FromJSON Trace where
  parseJSON (Object o)
    =   traceType o "wsgi"        *> (Wsgi       <$> o .: "info" <*> o .: "children")
    <|> traceType o "db"          *> (DB         <$> o .: "info" <*> o .: "children")
    <|> traceType o "rpc"         *> (RPC        <$> o .: "info" <*> o .: "children")
    <|> traceType o "compute_api" *> (ComputeApi <$> o .: "info" <*> o .: "children")
    <|> traceType o "nova_image"  *> (NovaImage  <$> o .: "info" <*> o .: "children")
    <|> traceType o "vif_driver"  *> (NovaVirt   <$> o .: "info" <*> o .: "children")
    <|> traceType o "neutron_api" *> (NeutronApi <$> o .: "info" <*> o .: "children")

    where
      traceType :: Object -> String -> Parser String
      traceType o n = do
        o' <- o  .: "info" -- :: Parser Object
        v' <- o' .: "name" -- :: Parser String
        if v' == n
          then pure v'
          else mzero
  parseJSON _          = empty


seqdiag :: Trace -> String
seqdiag t = concat $ seqdiag' serviceName t (getChildren t)
  where
    seqdiag' :: (Trace -> String) -> Trace -> [Trace] -> [String]
    seqdiag' f t = map (\t' -> f t ++ " => " ++ f t' ++
                         if null (getChildren t') then ";\n"
                         else " {\n " ++ seqdiag t' ++ " } ")

    serviceName :: Trace -> String
    serviceName (Wsgi       ti _) = project ti ++ ":WSGI"
    serviceName (DB         ti _) = project ti ++ ":DB"
    serviceName (RPC        ti _) = project ti ++ ":RPC"
    serviceName (ComputeApi ti _) = project ti ++ ":ComputeApi"
    serviceName (NovaImage  ti _) = project ti ++ ":NovaImage"
    serviceName (NovaVirt   ti _) = project ti ++ ":NovaVirt"
    serviceName (NeutronApi ti _) = project ti ++ ":NeutronApi"


    getChildren :: Trace -> [Trace]
    getChildren (Wsgi _ ts) = ts
    getChildren (DB _ ts) = ts
    getChildren (RPC _ ts) = ts
    getChildren (ComputeApi _ ts) = ts
    getChildren (NovaImage _ ts) = ts
    getChildren (NovaVirt _ ts) = ts
    getChildren (NeutronApi _ ts) = ts

seqdiagTop :: [Trace] -> String
seqdiagTop ts = "seqdiag {\n" ++ concat (map seqdiag ts) ++ "\n}"


-- API
decodeTrace :: BLI.ByteString -> Maybe [Trace]
decodeTrace = decodeWith json (parse parserTopTrace)

eitherDecodeTrace :: BLI.ByteString -> Either String [Trace]
eitherDecodeTrace = eitherFormatError . eitherDecodeWith json (iparse parserTopTrace)
  where
    eitherFormatError :: Either (JSONPath, String) a -> Either String a
    eitherFormatError = either (Left . uncurry formatError) Right

main :: IO ()
main = do
  json <- BS.readFile "test/rsc/server-create-real.json"
  writeFile "/tmp/test" (maybe "nothing" seqdiagTop (decodeTrace json))
