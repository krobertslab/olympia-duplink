package edu.uth.sbmi.olympia.duplink;

import edu.uth.sbmi.olympia.text.*;
import edu.uth.sbmi.olympia.util.*;
import edu.uth.sbmi.olympia.util.align.*;
import edu.uth.sbmi.olympia.util.io.*;
import edu.uth.sbmi.olympia.util.xml.*;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.logging.*;

import org.jdom2.Element;

/**
 * Command line functionality for {@link DupLink}.
 *
 * @author Kirk Roberts - kirk.roberts@uth.tmc.edu
 */
public class RunDupLink {
  private static final Log log = new Log(RunDupLink.class);
  public static boolean DEFAULT_TOKENIZED = false;

  public static void main(String[] argv) throws Exception {
    argv = Config.init("olympia.properties", argv);

    final Logger baseLogger = Logger.getLogger(RunDupLink.class.getName());
    baseLogger.setUseParentHandlers(false);

    final ConsoleHandler handler = new ConsoleHandler();
    handler.setLevel(Level.ALL);
    handler.setFormatter(new LogFormatter(false));
    baseLogger.addHandler(handler);

    final String argumentStructure =
        "Command: duplink [documents] [output] [parameters]\n" +
        "    documents:   [mandatory] document directory (see below)\n" +
        "    output:      [mandatory] output annotations (see below)\n" +
        "    --gap:       [optional]  gap penalty (default: " + DupLink.DEFAULT_GAP + ") for insertions/deletions, must be <= 0.0\n" +
        "    --penalty:   [optional]  similarity penalty (default: " + DupLink.DEFAULT_PENALTY + ") for changes, must be <= 0.0\n" +
        "    --minScore:  [optional]  minimum score (default: " + DupLink.DEFAULT_MIN_SCORE + ") to trigger a duplicate span, must be >0\n" +
        "    --tokenized: [optional]  whether the text is already space-tokenized (default: " + DEFAULT_TOKENIZED + "), if not " +
                                     "the default Olympia tokenizer will be used instead. Value must be either 'true' or 'false'.\n" +
        "    --logging:   [optional]  logging level, options: [FINE, FINER, FINEST]\n" +
        "\n" +
        "Example: duplink documents/ duplink_out.txt --gap -1 --penalty -2 --minScore 25\n" +
        "\n" +
        "Document Directory: contains files with numeric names (e.g., 0090234) with optional .txt extension.  " +
            "The file names, when sorted numerically, correspond to the temporal order of the documents.  " +
            "(E.g., milliseconds since 1970 format, YYYYMMDDhhmmss format, or anything else that indicates " +
                "the temporal order of the documents.)\n" +
        "\n" +
        "Document Structure: simple text file with no markup, optionally tokenized (see --tokenized)\n" +
        "\n" +
        "Output Annotations: list of duplicated spans in a four-column space-separated file:\n" +
        "    [document_id] [duplicate_id] [char_start] [char_end]\n" +
        "  document_id:  corresponds to the filename from the input folder\n" +
        "  duplicate_id: identifier provided such that all spans with the same duplicate_id are considered duplicates\n" +
        "  char_start:   inclusive start character offset from the original file\n" +
        "  char_end:     exclusive end character offset from the original file\n" +
        "  overlap_per:  percent overlap of the original source (by tokens)\n";

    if (argv.length < 2 || argv.length % 2 != 0) {
      log.severe("Improper number of arguments: {0}", argv.length);
      log.severe("{0}", argumentStructure);
      System.exit(1);
    }

    final Place input = Place.fromFile(argv[0]);
    if (input.isDirectory() == false) {
      log.severe("Not a directory: {0}", input);
      log.severe("{0}", argumentStructure);
      System.exit(1);
    }
    final List<? extends Place> inputFiles = input.getSortedChildren(
        Place.NUMERIC_NAME_COMPARATOR);
    if (inputFiles.isEmpty()) {
      log.severe("Empty directory: {0}", input);
      log.severe("{0}", argumentStructure);
      System.exit(1);
    }

    final Place output = Place.fromFile(argv[1]);
    if (output.exists()) {
      log.severe("Output file already exists: {0}", output);
      log.severe("{0}", argumentStructure);
      System.exit(1);
    }
    final Writer writer;
    try {
      writer = output.openWriter();
      writer.write("document_id duplicate_id char_start char_end overlap_per\n");
      writer.flush();
    }
    catch (IOException ioe) {
      log.severe("Could not write to file: {0}", output);
      log.severe("{0}", argumentStructure);
      System.exit(1);
      return;
    }

    double gap = DupLink.DEFAULT_GAP;
    double penalty = DupLink.DEFAULT_PENALTY;
    double minScore = DupLink.DEFAULT_MIN_SCORE;
    boolean tokenized = DEFAULT_TOKENIZED;
    Place detailFile = null;
    for (int i = 2; i < argv.length; i += 2) {
      if (argv[i].equals("--gap")) {
        try {
          gap = Double.valueOf(argv[i+1]);
        }
        catch (NumberFormatException nfe) {
          log.severe("Invalid gap value: {0}", argv[i+1]);
          log.severe("{0}", argumentStructure);
          System.exit(1);
        }
        if (gap > 0.0) {
          log.severe("Invalid gap value: {0}", argv[i+1]);
          log.severe("{0}", argumentStructure);
          System.exit(1);
        }
      }
      else if (argv[i].equals("--penalty")) {
        try {
          penalty = Double.valueOf(argv[i+1]);
        }
        catch (NumberFormatException nfe) {
          log.severe("Invalid penalty value: {0}", argv[i+1]);
          log.severe("{0}", argumentStructure);
          System.exit(1);
        }
        if (penalty > 0.0) {
          log.severe("Invalid penalty value: {0}", argv[i+1]);
          log.severe("{0}", argumentStructure);
          System.exit(1);
        }
      }
      else if (argv[i].equals("--minScore")) {
        try {
          minScore = Double.valueOf(argv[i+1]);
        }
        catch (NumberFormatException nfe) {
          log.severe("Invalid minScore value: {0}", argv[i+1]);
          log.severe("{0}", argumentStructure);
          System.exit(1);
        }
        if (minScore <= 0.0) {
          log.severe("Invalid minScore value: {0}", argv[i+1]);
          log.severe("{0}", argumentStructure);
          System.exit(1);
        }
      }
      else if (argv[i].equals("--tokenized")) {
        final String value = argv[i+1].toLowerCase();
        if (value.equals("true")) {
          tokenized = true;
        }
        else if (value.equals("false")) {
          tokenized = false;
        }
        else {
          log.severe("Invalid tokenized value: {0}", argv[i+1]);
          log.severe("{0}", argumentStructure);
          System.exit(1);
        }
      }
      else if (argv[i].equals("--logging")) {
        final String value = argv[i+1].toUpperCase();
        if (value.equals("FINE")) {
          log.setLevel(Log.FINE);
        }
        else if (value.equals("FINER")) {
          log.setLevel(Log.FINER);
        }
        else if (value.equals("FINEST")) {
          log.setLevel(Log.FINEST);
        }
        else {
          log.severe("Invalid logging value: {0}", argv[i+1]);
          log.severe("{0}", argumentStructure);
          System.exit(1);
        }
      }
      else if (argv[i].equals("--details")) {
        detailFile = Place.fromFile(argv[i+1]);
      }
      else {
        log.severe("Unknown argument: {0}", argv[i]);
        log.severe("{0}", argumentStructure);
        System.exit(1);
      }
    }
    if (tokenized) {
      Config.get("edu.uth.sbmi.olympia.text.annotator.TOKEN")
            .set("edu.uth.sbmi.olympia.text.annotators.WhitespaceTokenizer");
    }

    final List<Document> documents = new ArrayList<>();
    for (final Place file : inputFiles) {
      log.fine("File: {0}", file);
      if (file.isDirectory()) {
        log.severe("Is a directory, not a text file: {0}", file);
        log.severe("{0}", argumentStructure);
        System.exit(1);
      }
      final String filename = file.getName().replace(".txt", "");
      if (Strings.isAllDigits(filename) == false) {
        log.severe("Improper file name: {0}", file);
        log.severe("{0}", argumentStructure);
        System.exit(1);
      }
      try {
        final Long timestamp = Long.valueOf(filename);
      }
      catch (NumberFormatException nfe) {
        log.severe("Improper file name: {0}", file);
        log.severe("{0}", argumentStructure);
        System.exit(1);
      }
      
      final String text;
      try {
        text = file.readString();
      }
      catch (IOException ioe) {
        log.severe("Could not read file: {0}", file);
        log.severe("{0}", argumentStructure);
        System.exit(1);
        continue;
      }

      final Document document = new Document(text);
      document.setDocumentID(filename);
      document.setFile(file);
      document.annotate(Token.TYPE);
      documents.add(document);
    }

    final DupLink dupLink = new DupLink(gap, penalty, minScore);
    dupLink.findDuplicates(documents);

    final Map<String,List<Link>> duplicateGroups = new LinkedHashMap<>();
    final List<Link> allLinks = new ArrayList<>();
    for (final Document document : documents) {
      final Collection<Link> links = document.getSub(Link.class);
      log.fine("Document: {0}  ({1} tokens, {2} links)",
          document.getDocumentID(), document.getTokenLength(), links.size());
      int linknum = 1;
      for (final Link link : document.getSub(Link.class)) {
        log.finer("Link {0}  ({1} diffs)", linknum, link.getDiffs().size());
        final Text src = link.getSource();
        final Text dest = link.getDestination();
        final String srcID = src.getDocumentID();
        final String destID = dest.getDocumentID();
        final String srcKey = srcID + ":" +
            src.getStartCharOffset() + "-" + src.getEndCharOffset();
        if (duplicateGroups.containsKey(srcKey) == false) {
          duplicateGroups.put(srcKey, new ArrayList<Link>());
        }
        duplicateGroups.get(srcKey).add(link);
        allLinks.add(link);
        // Logging
        if (log.finer()) {
            log.finer("  Source:      {0} ({1} tokens)", srcID,
                link.getSource().getTokenLength());
            final String srcStr = src.asRawString();
            log.finer("      {0}", Strings.join(
                Strings.wrapLines(srcStr, 120, true), "\n      ").trim());
            log.finer("  Destination: {0} ({1} tokens)", destID,
                link.getDestination().getTokenLength());
            final String destStr = dest.asRawString();
            log.finer("      {0}", Strings.join(
                Strings.wrapLines(destStr, 120, true), "\n      ").trim());
          int diffnum = 1;
          for (final Pair<Text,Text> diff : link.getDiffs()) {
            log.finest("  Diff {0}", diffnum);
            final Text one = diff.getFirst();
            final Text two = diff.getSecond();
            log.finest("    {0}: {1} tokens", srcID,
                one == null ? "null" : one.getTokenLength());
            final String oneStr = one.asRawString();
            log.finest("        {0}", Strings.join(
                Strings.wrapLines(oneStr, 120, true), "\n        ").trim());
            log.finest("    {0}: {1} tokens", destID,
                two == null ? "null" : two.getTokenLength());
            final String twoStr = two.asRawString();
            log.finest("        {0}", Strings.join(
                Strings.wrapLines(twoStr, 120, true), "\n        ").trim());
            diffnum++;
          }
        }
        linknum++;
      }
    }

    final DecimalFormat OVERLAP_FORMAT = new DecimalFormat("0.00");
    final Map<Link,String> clusterIDs = new HashMap<>();
    try {
      final Set<String> md5Sanity = new HashSet<>();
      for (final Map.Entry<String,List<Link>> e : duplicateGroups.entrySet()) {
        final String srcKey = e.getKey();
        final List<Link> links = e.getValue();

        final String clusterID = IOUtil.md5sum(srcKey).substring(0, 8);
        assert md5Sanity.add(clusterID);
        final Text src = links.get(0).getSource();
        writer.write(src.getDocumentID() + " " + clusterID + " " +
            src.getStartCharOffset() + " " + src.getEndCharOffset() + " *\n");

        for (final Link link : links) {
          final String prev = clusterIDs.put(link, clusterID);
          assert prev == null;

          final Text dest = link.getDestination();

          final int srcLen = src.getTokenLength();
          int tokenOverlap = srcLen;
          for (final Pair<Text,Text> diff : link.getDiffs()) {
            final Text srcDiff = diff.getFirst();
            assert src.getDocument() == srcDiff.getDocument();
            tokenOverlap -= srcDiff.getTokenLength();
          }
          final double tokenOverlapPer =
              100.0 * tokenOverlap / src.getTokenLength();

          writer.write(dest.getDocumentID() + " " + clusterID + " " +
              dest.getStartCharOffset() + " " + dest.getEndCharOffset() + " " +
              OVERLAP_FORMAT.format(tokenOverlapPer) + "\n");
        }
      }
      writer.close();
    }
    catch (IOException ioe) {
      log.severe("Could not write to file: {0}", output);
      log.severe("{0}", argumentStructure);
      System.exit(1);
    }

    if (detailFile != null) {
      final Element root = new Element("Documents");
      root.addContent(new org.jdom2.Text("\n"));
      for (final Document document : documents) {
        final Element documentElem = new Element("Document");
        documentElem.setAttribute("document_id", document.getDocumentID());
        final String rawString = document.asRawString();

        final List<Text> spans = new ArrayList<>();
        spans.addAll(document.getNonIntersecting(Link.class));
        spans.addAll(document.getSub(Link.class));
        Collections.sort(spans, TextComparators.startToken());

        for (int i = 0; i < spans.size(); i++) {
          final Text span = spans.get(i);
          if (i > 0) {
            final Text prevSpan = spans.get(i-1);
            documentElem.addContent(new org.jdom2.Text(
                rawString.substring(prevSpan.getEndCharOffset(),
                                    span.getStartCharOffset())));
          }

          int startChar = span.getStartCharOffset();
          int endChar = span.getEndCharOffset();
          if (span instanceof Link) {
            final Link link = (Link) span;
            final Element linkElem = new Element("Duplicate");
            linkElem.setAttribute("source-document_id",
                link.getSource().getDocumentID());
            linkElem.setAttribute("source-char_start",
                Integer.toString(link.getSource().getStartCharOffset()));
            linkElem.setAttribute("source-char_end",
                Integer.toString(link.getSource().getEndCharOffset()));
            linkElem.setText(rawString.substring(startChar, endChar));
            documentElem.addContent(linkElem);
          }
          else {
            if (span.hasPrevToken() == false) {
              startChar = 0;
            }
            if (span.hasNextToken() == false) {
              endChar = rawString.length();
            }
            documentElem.addContent(new org.jdom2.Text(
                rawString.substring(startChar, endChar)));
          }
        }

        root.addContent(documentElem);
        root.addContent(new org.jdom2.Text("\n"));
      }
      try {
        XMLUtil.writeFile(root, detailFile, XMLUtil.RAW_FORMAT);
      }
      catch (IOException ioe) {
        log.severe("Could not write to file: {0}", output);
        log.severe("{0}", argumentStructure);
        System.exit(1);
      }
    }

  }

}
