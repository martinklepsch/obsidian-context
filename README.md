# Obsidian Context

`obsidian-context` is a CLI script to query and export content from your Obsidian vault for use with LLMs.

It can list `recent-links` (recently modified files) and `pack` your files (a la [repomix](https://repomix.com/)) into a single context blob. For now it's meant to support two main workflows:

1. **Pack Files**: Easily pack files I edited this week/month for use with LLMs.
2. **List Links**: Get a list of frequently used links that I might want to consider as options when auto-tagging content in my vault / adding new content.

> `obsidian-context` works fully offline and will not modify any files.

## Installation

```sh
brew install babashka/brew/bbin
bbin install io.github.martinklepsch/obsidian-context
```

`obsidian-context` is a [babashka](https://github.com/babashka/babashka) program, using [bbin](https://github.com/babashka/bbin) for distribution.

## Usage

The tool relies on a location to an Obsidian vault, this can be provided via the `$OBSIDIAN_DIR` environment variable.

```sh
export OBSIDIAN_DIR=/path/to/your-vault
```

### Pack Files

Combine files modified within a specified timeframe into a single text blob (useful for feeding context to LLMs).

**Pack all files:**
```sh
obsidian-context pack
```

**Pack files modified in the last 7 days:**
```sh
obsidian-context pack --lookback 7d
```

**Pack files modified in the last 3 months:**
```sh
obsidian-context pack --lookback 3m
```

*(You can pipe the output to `less` for easier viewing: `obsidian-context pack --lookback 3m | less`)*


### List Recent Links

Get a list of recently modified files, sorted by modification time (newest first). This is useful for seeing recent activity or providing an LLM with potentially relevant recent notes.

> [!NOTE]
> Currently only links to existing files are shown. Links to files that are not found in the vault are not listed.

**Default (show recently modified files):**
```sh
obsidian-context recent-links
```

This defaults to `--lookback 30d` and `--limit 100`.

**List the 20 most recently modified files:**
```sh
obsidian-context recent-links --limit 20
```

**Links for files modified in the last 7 days:**
```sh
obsidian-context recent-links --lookback 7d
```

**List the top 10 files modified in the last 60 days, excluding empty files and daily notes:**
```sh
obsidian-context recent-links --lookback 60d --exclude-daily-notes --limit 10
```

### Getting Help

**General help:**
```sh
obsidian-context --help
```
or simply:
```sh
obsidian-context
```

**Help for a specific command (e.g., `pack`):**
```sh
obsidian-context pack --help
```
