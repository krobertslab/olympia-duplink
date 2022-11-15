package edu.uth.sbmi.olympia.duplink;

import edu.uth.sbmi.olympia.text.*;
import edu.uth.sbmi.olympia.util.*;

import java.io.*;
import java.util.*;

/**
 * Link between a duplicate section and the original {@link Text}.
 *
 * @author Kirk Roberts - kirk.roberts@uth.tmc.edu
 */
public class Link extends Annotation {
  private static final Log log = new Log(Link.class);

  private final Text source;
  private final List<Pair<Text,Text>> diffs = new ArrayList<>();

  /**
   * Creates a new <code>Link</code> from the given <var>duplicate</var> and
   * <var>source</var> {@link Text}.
   */
  public Link(final Text duplicate, final Text source) {
    super(duplicate);
    this.source = source;
  }

  /**
   * Returns the source {@link Text}.
   */
  public Text getSource() {
    return source;
  }

  /**
   * Returns the destination {@link Text}.
   */
  public Text getDestination() {
    return this;
  }

  /**
   * Adds a difference between the source and destination {@link Text}s.
   */
  public void addDiff(final Text srcText, final Text destText) {
    diffs.add(Pair.of(srcText, destText));
  }

  /**
   * Returns the differences between the source and destination {@link Text}s.
   */
  public List<Pair<Text,Text>> getDiffs() {
    return Collections.unmodifiableList(diffs);
  }

}
