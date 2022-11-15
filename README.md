# olympia-quehry

DupLink - Duplicate text identifier (and copy-paste detector)

## Dependencies

The project is tested on linux (Ubuntu 20.04).

### Jar files

Download the following Java Archive files and place under the [`lib` directory](lib).

* [jdom2-2.0.3.jar](https://repo1.maven.org/maven2/org/jdom/jdom2/2.0.3/jdom2-2.0.3.jar)

## Compile

```shell
ant
```

## Run

Command Line Operation:

```shell
bin/duplink [documents] [output] [parameters]
```

Command Line Parameters:
```
Command: duplink [documents] [output] [parameters]
    documents:   [mandatory] document directory (see below)
    output:      [mandatory] output annotations (see below)
    --gap:       [optional]  gap penalty (default: -5.0) for insertions/deletions, must be <= 0.0
    --penalty:   [optional]  similarity penalty (default: -10.0) for changes, must be <= 0.0
    --minScore:  [optional]  minimum score (default: 50.0) to trigger a duplicate span, must be >0
    --tokenized: [optional]  whether the text is already space-tokenized (default: false), if not the default Olympia tokenizer will be used instead. Value must be either 'true' or 'false'.
    --logging:   [optional]  logging level, options: [FINE, FINER, FINEST]

Example: duplink documents/ duplink_out.txt --gap -1 --penalty -2 --minScore 25

Document Directory: contains files with numeric names (e.g., 0090234) with optional .txt extension.  The file names, when sorted numerically, correspond to the temporal order of the documents.  (E.g., milliseconds since 1970 format, YYYYMMDDhhmmss format, or anything else that indicates the temporal order of the documents.)

Document Structure: simple text file with no markup, optionally tokenized (see --tokenized)

Output Annotations: list of duplicated spans in a four-column space-separated file:
    [document_id] [duplicate_id] [char_start] [char_end]
  document_id:  corresponds to the filename from the input folder
  duplicate_id: identifier provided such that all spans with the same duplicate_id are considered duplicates
  char_start:   inclusive start character offset from the original file
  char_end:     exclusive end character offset from the original file
  overlap_per:  percent overlap of the original source (by tokens)
```

There is a small test dataset included from a synthetic note created as part of the [TREC Clinical Trials 2021 track](https://www.trec-cds.org/2021.html), which can be used for testing.  Recommended command for running that dataset:

```shell
./bin/duplink data/duplink/test_documents/ duplink_out.txt --gap -1 --penalty -1 --minScore 3 --logging finest --details duplink_details.xml
```
