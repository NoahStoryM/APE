{ pkgs ? (import <nixpkgs> {})
, compiler ? pkgs.haskell.packages.ghc822
}:

# with (import <nixpkgs/pkgs/development/haskell-modules/lib.nix> { inherit pkgs; });

pkgs.mkShell {
  buildInpupt = [ pkgs.stack pkgs.zlib pkgs.jq ];
  shellHook = ''
    COMPILER_TOOL_PATH=$(${pkgs.stack}/bin/stack --nix path --compiler-tools-bin)
    function stack_install_tool() {
        local haskell_pkg="$1"
        local executable="$2"; if [ -z $2 ]; then executable=$haskell_pkg; fi
        if [[ -e "$COMPILER_TOOL_PATH/$executable" ]]
        then
            echo "$haskell_pkg already installed"
        else
            ${pkgs.stack}/bin/stack --nix build --copy-compiler-tool "$haskell_pkg"
        fi
    }

    # Spacemacs dependencies for haskell env
    stack_install_tool ghc-mod
    stack_install_tool apply-refact refactor
    stack_install_tool hlint
    stack_install_tool stylish-haskell
    stack_install_tool hasktags

    # Put stack, ghc and stack-tools into the PATH
    PATH=$PATH:${pkgs.stack}/bin                   # stack
    PATH=$PATH:$(stack path --compiler-bin)        # ghc, ghci
    alias stack="${pkgs.stack}/bin/stack --nix"    # Fallback on nix by default

    PATH=$PATH:$COMPILER_TOOL_PATH                 # env tools, see spacemacs below

    echo ""
    echo "Command 'stack' is an alias for 'stack --nix'."
    echo "Run 'stack exec os-konan-exe' for Main app."
    echo "Run 'stack build --test :os-konan-test' for tests."
    echo "Run 'stack ide targets' for the list of available targets."
  '';
}
