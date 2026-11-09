{
  perSystem = { pkgs, ... }: {
    devShells.default = pkgs.mkShell {
      packages = [
        pkgs.clojure-lsp
        pkgs.babashka
      ];
    };
  };
}
