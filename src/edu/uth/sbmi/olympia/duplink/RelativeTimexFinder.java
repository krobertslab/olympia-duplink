package edu.uth.sbmi.olympia.duplink;

import edu.uth.sbmi.olympia.text.*;
import edu.uth.sbmi.olympia.util.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Finds relative temporal expressions.
 *
 * @author Kirk Roberts - kirk.roberts@uth.tmc.edu
 */
public class RelativeTimexFinder {
  private static final Log log = new Log(RelativeTimexFinder.class);
  private static final String NUMBERS = "(?:[0-9]+|one|two|three|four|five|six|" +
      "seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|" +
      "seventeen|eighteen|nineteen|twenty|thirty|fourty|fifty|sixty|seventy|" +
      "eighty|ninety|hundred|thousand|million|billion)";
  private static final String NUMBERS2 = "(?:" + NUMBERS + " ?)+";
  private static final String YEAR_UNITS = "y|y\\.|yr|yr\\.|yrs|yrs\\.|year|years";
  private static final String UNITS = "(?:hr|hrs|hour|hours|d|day|days|" +
      "night|nights|evening|evenings|morning|mornings|afternoon|afternoons|" +
      "w|wk|wks|week|weeks|mo|mo\\.|mos|mos\\.|month|months|summer|summers|" +
      "winter|winters|spring|springs|fall|falls|autumn|autumns|season|seasons" +
      "|"+YEAR_UNITS+"|decade|decades)";
  private static final String DAYOFWEEK = "monday|tuesday|wednesday|thursday|" +
      "friday|saturday|sunday";
  private static final String MONTHS = "january|february|march|april|may|june" +
      "|july|august|september|october|november|december";
  private static final String TAIL = "ago";
  private static final String HEAD = "past|previous|following|last|prior";
  private static final Pattern REL_PATTERN1 = Pattern.compile(
      "(" + NUMBERS2 + ")\\s+(" + UNITS + ")\\s+(" + TAIL + ")",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern REL_PATTERN2 = Pattern.compile(
      "(" + HEAD + ")\\s+(" + NUMBERS2 + ")\\s+(" + UNITS + ")",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern REL_PATTERN3 = Pattern.compile(
      "(last)\\s+(" + UNITS + ")", Pattern.CASE_INSENSITIVE);
  private static final Pattern REL_PATTERN4 = Pattern.compile(
      "(on)\\s+(" + DAYOFWEEK + ")", Pattern.CASE_INSENSITIVE);
  private static final Pattern REL_PATTERN5 = Pattern.compile(
      "(today|yesterday|tomorrow|tonight|currently|" +
      "the (?:present|current) time)", Pattern.CASE_INSENSITIVE);
  private static final Pattern REL_PATTERN6 = Pattern.compile(
      "(this)\\s+(am|pm|time|" + UNITS + "|" + DAYOFWEEK + "|" + MONTHS + ")",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern REL_PATTERN7 = Pattern.compile(
      "(some|several|many|few|couple)\\s+(" + UNITS + ")\\s+(" + TAIL + ")",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern AGE_PATTERN = Pattern.compile(
      "(" + NUMBERS2 + ")\\s+" +
      "(?:" + YEAR_UNITS + ")[ /-]?(?:o|o\\.|old)");

  /**
   * Returns the relative temporal expressions.
   */
  public List<Timex> findRelTimex(final Text text) {
    final List<Timex> timexes = new ArrayList<>();
    for (final Text span : text.findAll(REL_PATTERN1)) {
      final Timex timex = new Timex(span, UUID.randomUUID().toString());
      timex.addNote("relDate1");
      timexes.add(timex);
    }
    for (final Text span : text.findAll(REL_PATTERN2)) {
      final Timex timex = new Timex(span, UUID.randomUUID().toString());
      timex.addNote("relDate2");
      timexes.add(timex);
    }
    for (final Text span : text.findAll(REL_PATTERN3)) {
      final Timex timex = new Timex(span, UUID.randomUUID().toString());
      timex.addNote("relDate3");
      timexes.add(timex);
    }
    for (final Text span : text.findAll(REL_PATTERN4)) {
      final Timex timex = new Timex(span, UUID.randomUUID().toString());
      timex.addNote("relDate4");
      timexes.add(timex);
    }
    for (final Text span : text.findAll(REL_PATTERN5)) {
      final Timex timex = new Timex(span, UUID.randomUUID().toString());
      timex.addNote("relDate5");
      timexes.add(timex);
    }
    for (final Text span : text.findAll(REL_PATTERN6)) {
      if (span.asRawString().equalsIgnoreCase("this may")) {
        continue;
      }
      final Timex timex = new Timex(span, UUID.randomUUID().toString());
      timex.addNote("relDate6");
      timexes.add(timex);
    }
    for (final Text span : text.findAll(REL_PATTERN7)) {
      final Timex timex = new Timex(span, UUID.randomUUID().toString());
      timex.addNote("relDate7");
      timexes.add(timex);
    }
    return timexes;
  }

  /**
   * Returns the relative temporal expressions.
   */
  public List<Timex> findAge(final Text text) {
    final List<Timex> timexes = new ArrayList<>();
    for (final Text span : text.findAll(AGE_PATTERN)) {
      final Timex timex = new Timex(span, UUID.randomUUID().toString());
      timexes.add(timex);
    }
    return timexes;
  }

  /**
   * Command line (testing).
   */
  public static void main(String[] argv) throws Exception {
    argv = Config.init("experimental2.properties", argv);

    final List<String> testCases = Arrays.asList(
         "2 days ago",
         "10 weeks ago",
         "ten years ago",
         "fifteen months ago",
         "twenty four weeks ago");
    for (final String testCase : testCases) {
      log.info("Test Case: {0}", testCase);
      final Matcher m = REL_PATTERN1.matcher(testCase);
      if (m.matches()) {
        log.info("    --> matches");
      }
      else {
        log.warning(" --> didn't match");
      }
    }
  }

}
