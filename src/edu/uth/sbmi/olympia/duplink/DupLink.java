package edu.uth.sbmi.olympia.duplink;

import edu.uth.sbmi.olympia.text.*;
import edu.uth.sbmi.olympia.util.*;
import edu.uth.sbmi.olympia.util.align.*;

import java.io.*;
import java.util.*;

/**
 * Duplication detection that links duplicate sections to their original 
 *
 * @author Kirk Roberts - kirk.roberts@uth.tmc.edu
 */
public class DupLink {
  private static final Log log = new Log(DupLink.class);
  public static double DEFAULT_GAP = -5.0;
  public static double DEFAULT_PENALTY = -10.0;
  public static double DEFAULT_MIN_SCORE = 50.0;

  private final double gap;
  private final double minScore;
  private final double penalty;
  private final SimilarityMatrix sim = new SimilarityMatrix() {
    @Override
    public double similarity(final String item1, final String item2) {
      if (item1.equals(item2)) {
        return 1.0;
      }
      else {
        return penalty;
      }
    }
  };

  /**
   * Creates a new <code>DupLink</code> with the default parameters.
   */
  public DupLink() {
    this(DEFAULT_GAP, DEFAULT_PENALTY, DEFAULT_MIN_SCORE);
  }

  /**
   * Creates a new <code>DupLink</code> with the given <var>gap</var>,
   * <var>penalty</var>, and <var>minScore</var> parameters.
   */
  public DupLink(final double gap, final double penalty, final double minScore) {
    this.gap = gap;
    this.penalty = penalty;
    this.minScore = minScore;
  }

  /**
   * Creates a <code>String</code> array to use for Smith-Waterman.
   */
  private String[] getSequence(final Text text) {
    final List<Token> tokens = text.getTokens();
    final String[] sequence = new String[tokens.size()];
    for (int i = 0; i < sequence.length; i++) {
      sequence[i] = tokens.get(i).asRawString();
    }
    return sequence;
  }

  /**
   * Returns a <code>Set</code> of all the items in the <var>sequence</var>.
   */
  private Set<String> wordSet(final String[] sequence) {
    final Set<String> wordSet = new HashSet<>();
    for (int i = 0; i < sequence.length; i++) {
      wordSet.add(sequence[i]);
    }
    return wordSet;
  }

  ///**
  // * Returns the segments of the {@link Document} that are eligible for
  // * consideration as duplicates.
  // */
  //private List<Text> getEligibleSegments(final Document document) {
  //  final List<Text> segments = new ArrayList<>();

  //  final List<Link> existingLinks = new ArrayList<>(
  //      document.getSub(Link.class));
  //  
  //  if (existingLinks.isEmpty()) {
  //    segments.add(document);
  //    return segments;
  //  }

  //  segments.addAll(document.getNonIntersecting(Link.class));

  //  // Begin DBG
  //  final List<Text> DBG = new ArrayList<>(segments);
  //  DBG.addAll(document.getSub(Link.class));
  //  Collections.sort(DBG, TextComparators.startToken());
  //  for (final Text span : DBG) {
  //    if (span instanceof Link) {
  //      log.DBG("Paste Segment:");
  //    }
  //    else {
  //      log.DBG("Original Segment:");
  //    }
  //    log.DBG("  {0}", Strings.join(
  //        Strings.wrapLines(span.asRawString(), 120, true), "\n  ").trim());
  //  }
  //  System.exit(1);
  //  // End DBG

  //  return segments;
  //}

  /**
   * Annotates duplicate {@link Link}s on the given chronologically-ordered
   * {@link Document}s.
   */
  public void findDuplicates(final List<Document> documents) {
    final SmithWaterman aligner = new SmithWaterman();
    for (int x = 0; x < documents.size(); x++) {
      final Document doc1 = documents.get(x);
      final String[] seq1 = getSequence(doc1);
      final Set<String> wordSet1 = wordSet(seq1);
      for (int y = x + 1; y < documents.size(); y++) {
        final Document doc2 = documents.get(y);
        //for (final Text segment : getEligibleSegments(doc2)) {
        for (final Text segment : doc2.getNonIntersecting(Link.class)) {
          final int shift = segment.getStartTokenOffset();
          final String[] seq2 = getSequence(segment);

          // Speed-up: check the set overlap first
          final Set<String> wordSet2 = wordSet(seq2);
          final int overlap = Util.intersection(wordSet1, wordSet2).size();
          if (overlap < minScore) {
            continue;
          }

          // TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO
          // TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO
          // TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO
          //
          // Instead of running the entire doc2 sequence, only run the
          // sub-sequences of doc2 that don't already have an existing link
          //
          // TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO
          // TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO
          // TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO

          log.finest("Running Local Sequence Alignment on Documents: ({0}, {1}) " +
              "  ({2} tokens, {3} tokens)", x, y, seq1.length, seq2.length);
          final List<LocalAlignment> localAlignments =
              aligner.alignMulti(seq1, seq2, gap, sim, minScore);
          for (final LocalAlignment localAlignment : localAlignments) {
            final Alignment alignment = localAlignment.getBestAlignment();
            log.finest("--------------------------------------------------");
            log.finer("Alignment: {0} - {1} [{2},{3}) from [{4},{5})",
                doc1.getDocumentID(), doc2.getDocumentID(),
                segment.getStartTokenOffset(), segment.getEndTokenOffset(),
                doc2.getStartTokenOffset(),    doc2.getEndTokenOffset());
            log.finer("Alignment Score: {0}", alignment.score);
            if (log.pico()) {
              log.pico("{0}", alignment.pretty());
            }

            final Text src = doc1.getToken(alignment.start1).union(
                             doc1.getToken(alignment.end1-1));
            final Text dest = doc2.getToken(shift + alignment.start2).union(
                              doc2.getToken(shift + alignment.end2-1));

            assert dest.hasIntersecting(Link.class) == false : "segmenting error";

            //// Only connect a destination to its earliest possible source, that
            //// means if there's already an attached Link, this one should be
            //// skipped unless there is a leading/trailing destination
            //if (dest.hasIntersecting(Link.class)) {
            //  log.finer("==> found transitive copy");
            //  if (log.finest()) {
            //    for (final Link link : dest.getIntersecting(Link.class)) {
            //      final Text _src = link.getSource();
            //      log.finest("Prior Source ({0})", _src.getDocumentID());
            //      log.finest("  {0}", Strings.join(
            //          Strings.wrapLines(_src.asRawString(), 120, true), "\n  ").trim());
            //    }
            //    log.finest("Current Source ({0})", src.getDocumentID());
            //    log.finest("  {0}", Strings.join(
            //        Strings.wrapLines(src.asRawString(), 120, true), "\n  ").trim());
            //  }

            //  if (dest.getFirstToken().hasSuper(Link.class) == false) {
            //    final Token first = dest.getFirstToken();
            //    Token last = first;
            //    while (last.getNextToken().hasSuper(Link.class) == false) {
            //      last = last.getNextToken();
            //      assert last != dest.getLastToken();
            //    }
            //    final Text span = first.union(last);
            //    log.DBG("Need to add Leading Destination:");
            //    log.DBG("  {0}", Strings.join(
            //        Strings.wrapLines(span.asRawString(), 120, true), "\n  ").trim());
            //  }
            //  if (dest.getLastToken().hasSuper(Link.class) == false) {
            //    final Token last = dest.getLastToken();
            //    Token first = last;
            //    while (first.getPrevToken().hasSuper(Link.class) == false) {
            //      first = first.getPrevToken();
            //      assert first != dest.getFirstToken();
            //    }
            //    final Text span = first.union(last);
            //    log.DBG("Need to add Trailing Destination:");
            //    log.DBG("  {0}", Strings.join(
            //        Strings.wrapLines(span.asRawString(), 120, true), "\n  ").trim());
            //  }
            //  System.exit(1);
            //}

            final Link link = new Link(dest, src);

            final List<Pair<Token,Token>> tokenDiffs = new ArrayList<>();
            int m = src.getStartTokenOffset();
            int n = dest.getStartTokenOffset();
            int numTokenDiffs = 0;
            for (int i = 0; i < alignment.sequence1.length; i++) {
              final String item1 = alignment.sequence1[i];
              final String item2 = alignment.sequence2[i];
              if (item1.equals(item2)) {
                m++;
                n++;
              }
              else if (item1 == Alignment.GAP) {
                tokenDiffs.add(Pair.of((Token) null, doc2.getToken(n)));
                n++;
                numTokenDiffs++;
              }
              else if (item2 == Alignment.GAP) {
                tokenDiffs.add(Pair.of(doc1.getToken(m), (Token) null));
                m++;
                numTokenDiffs++;
              }
              else {
                tokenDiffs.add(Pair.of(doc1.getToken(m), doc2.getToken(n)));
                m++;
                n++;
                numTokenDiffs += 2;
              }
            }

            if (tokenDiffs.size() > 0) {
              final List<Token> tokens1 = new ArrayList<>();
              final List<Token> tokens2 = new ArrayList<>();
              for (final Pair<Token,Token> tokenDiff : tokenDiffs) {
                final Token token1 = tokenDiff.getFirst();
                final Token token2 = tokenDiff.getSecond();
                if (token1 != null && token2 != null) {
                  if (tokens1.size() > 0 || tokens2.size() > 0) {
                    numTokenDiffs -= addDiff(link, tokens1, tokens2);
                    tokens1.clear();
                    tokens2.clear();
                  }
                  numTokenDiffs -= addDiff(link,
                      Collections.singletonList(token1),
                      Collections.singletonList(token2));
                }
                else if (token1 != null) {
                  tokens1.add(token1);
                }
                else if (token2 != null) {
                  tokens2.add(token2);
                }
              }
              if (tokens1.size() > 0 || tokens2.size() > 0) {
                numTokenDiffs -= addDiff(link, tokens1, tokens2);
                tokens1.clear();
                tokens2.clear();
              }
              assert numTokenDiffs == 0;
            }

            link.attach();
          }
        }
      }
    }
  }

  /**
   * Adds a text difference to the {@link Link}.  Returns the number of
   * {@link Token}s affected by the difference.
   */
  private static int addDiff(final Link link,
                             final List<Token> tokens1,
                             final List<Token> tokens2) {
    final Text text1 = tokens1.isEmpty() ? null :
        tokens1.get(0).union(tokens1.get(tokens1.size()-1));
    final Text text2 = tokens2.isEmpty() ? null :
        tokens2.get(0).union(tokens2.get(tokens2.size()-1));
    log.finer("adding diff: {0} vs {1}", text1, text2);
    link.addDiff(text1, text2);
    return tokens1.size() + tokens2.size();
  }

}
