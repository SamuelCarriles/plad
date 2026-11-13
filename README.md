# plad — Project Layout As Data

Materialize directory structures and files from EDN blueprints.

## Why

Describing a project's layout in plain data means you can version it, share
it, and generate it reproducibly. `plad` reads an EDN file and either creates
the described layout on disk, validates the file, or checks whether the layout
exists on a given directory.

Built to scratch my own itch for a simple, scriptable way to bootstrap and
verify project skeletons.

## Install

`plad` runs on [Babashka](https://babashka.org), so you need it installed
first. See [Babashka's install
guide](https://github.com/babashka/babashka#installation).

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

To upgrade later:

```bash
bbin install io.github.SamuelCarriles/plad --latest-tag
```

### Manual

Download the uberscript from the latest release:

```bash
curl -L https://github.com/SamuelCarriles/plad/releases/download/v0.1.0-alpha/plad -o plad
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

Generate the layout on disk.

```bash
plad gen layout.edn                   # create under current directory
plad gen layout.edn -o /tmp/foo       # create under a specific directory
plad gen layout.edn -d                # dry run: print what would be created
```

Files that already exist are left untouched.

### `check`

Compare a layout against the filesystem.

```bash
plad check layout.edn                  # check against current directory
plad check layout.edn -t /tmp/foo      # check against a specific directory
```

Prints each entry with its status (`[exists]` or `[missing]`) and a summary.
Exits with status 0 if everything is present, 1 if anything is missing.

### `validate`

Check that a layout file is well-formed.

```bash
plad validate layout.edn
```

Exits with status 0 if valid, 1 otherwise. Errors are reported with
[Expound](https://github.com/bhb/expound).

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
