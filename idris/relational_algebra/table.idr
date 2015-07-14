module sql.table

import row

%default total

-- infixr 7 |

data Table : Schema -> Type where
  TNil : Table s
  (::) : Row s -> Table s -> Table s

tableAgenda : Table scAgenda
tableAgenda = row1 :: row2 :: row3 :: TNil


π : (s' : Schema) -> Table s -> {auto p : s' `Sub` s} -> Table s'
π s' TNil = TNil
π s' (r :: rs) {p=p'} = row.π s' r {p=p'} :: π s' rs

attrId : Attr
attrId = (MkAttr "id" Integer)

frag : (s' : Schema) -> Table s -> {auto p : s' `Sub` s} -> (Table $ attrId :: s', Table $ attrId :: (s \\ s'))
frag s' TNil = (TNil, TNil)
frag s' (r :: rs) {p=p'} = let (rleft, rright) = row.frag s' r {p=p'} in
                           let (tleft, tright) = frag s' rs in
                           ((1 |: rleft) :: tleft, (1 |: rright) :: tright)