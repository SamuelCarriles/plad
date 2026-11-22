# plad — Project Layout As Data

![Status](https://img.shields.io/badge/status-alpha-orange)
![Version](https://img.shields.io/github/v/release/SamuelCarriles/plad?include_prereleases&label=version)

Materialize directory structures and files from EDN blueprints.

## Why

Describing a project's layout in plain data means you can version it, share
it, and generate it reproducibly. `plad` reads an EDN file and either creates
the described layout on disk (`gen`), asserts its structure and filesystem compatibility (`check`), or reports on its state (`status`).

Built to scratch my own itch for a simple, scriptable way to bootstrap and
verify project skeletons.

## Install

`plad` runs on [Babashka](https://babashka.org), so you need it installed
first. See [Babashka's install
guide](https://github.com/babashka/babashka#installation).

> [!IMPORTANT]
> `plad` was developed and tested with **Babashka 1.13.220**. Use that
> version or newer. On Babashka 1.12.x, running `plad` without arguments or
> with `--help` prints `nil` instead of the usage, because the `babashka.cli`
> bundled with that version does not support the built-in help.
> Check your version with `bb --version`.

### With bbin (recommended)

[bbin](https://github.com/babashka/bbin) installs Babashka scripts as
first-class commands:

```bash
bbin install io.github.SamuelCarriles/plad --latest-tag
```

Verify:

```bash
plad version
```

To upgrade later use the same install command.

### Manual

Download the uberscript from the latest pre-release:

```bash
curl -L https://github.com/SamuelCarriles/plad/releases/download/v0.1.0-alpha.3/plad -o plad
chmod +x plad
```

Move it somewhere in your `$PATH`:

```bash
mv plad ~/.local/bin/
```

Verify:

```bash
plad version
```

## Quick start

Write a layout file:

```edn
;; layout.edn
{:doc "My new project"
 :layout [["src/" ["my_app/" "core.clj"]]
          "deps.edn"
          "README.md"]}
```

Generate it:

```bash
plad gen layout.edn
```

You get:

```
src/
  my_app/
    core.clj
deps.edn
README.md
```

## Commands

### `gen`

Generate the layout on disk. Silent on success.

```bash
plad gen layout.edn                   # create in current directory
plad gen layout.edn -t /tmp/foo       # create in a specific directory
plad gen layout.edn -d                # dry run: preview what would be created
```

Existing matching files and directories are left untouched. Aborts if a type conflict occurs (e.g. a file exists where a directory is declared).

### `check`

Pure assertion to validate layout syntax, semantics, and filesystem compatibility. Silent on success.

```bash
plad check layout.edn                 # check layout against current directory
plad check layout.edn -t /tmp/foo     # check layout against a specific directory
plad check layout.edn --no-fs         # check syntax & semantics only (ignore filesystem)
```

Exits silently with status 0 if valid. Throws an error with details if the target directory does not exist or if there are type conflicts on disk.

### `status`

Print a human-readable report comparing a layout against the filesystem.

```bash
plad status layout.edn                # report status against current directory
plad status layout.edn -t /tmp/foo    # report status against a specific directory
```

Prints each entry with its status (`[exists]`, `[missing]`, or `[conflict]`) followed by a summary breakdown.

### `version`

Print the installed `plad` version.

```bash
plad version
```

## Layout file format

> [!NOTE]
> The EDN format is unstable while `plad` is in alpha and may change between releases.

An EDN map with:

- `:layout` (required) — a vector of nodes.
- `:doc` (optional) — a string describing the layout.

A **node** is either:

- A **string path**. A trailing `/` means it is a directory (e.g. `"src/"`);
  otherwise it is a file.
- A **vector** `[dir & children]` where `dir` is a directory path and
  `children` is a sequence of nodes nested inside it.

Example:

```edn
{:doc "A small example"
 :layout [["src/"
           ["my_app/"
            "core.clj"
            "utils.clj"
            ["http/"
             "server.clj"
             "routes.clj"]]]
          "deps.edn"
          "README.md"]}
```

More examples live in [`examples/`](./examples).

## Development

Run the test suite:

```bash
bb test
```

Build the uberscript:

```bash
bb build
```

This writes plad in the project root, prepends the `#!/usr/bin/env bb` shebang, and sets execute permissions, so you can run it as `./plad`.

## FAQ

**How is this different from deps-new?**
deps-new generates Clojure projects from templates, including file contents.
plad declares any directory structure as data (empty files/dirs), asserts it with `check`, and reports on its status with `status`. It's language-agnostic and intentionally minimal.
