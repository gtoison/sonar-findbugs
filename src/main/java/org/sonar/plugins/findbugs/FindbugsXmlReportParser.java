/*
 * SonarQube Findbugs Plugin
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.findbugs;

import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMInputCursor;

import javax.annotation.CheckForNull;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class FindbugsXmlReportParser {

  private final File findbugsXmlReport;
  private final String findbugsXmlReportPath;

  public FindbugsXmlReportParser(File findbugsXmlReport) {
    this.findbugsXmlReport = findbugsXmlReport;
    findbugsXmlReportPath = findbugsXmlReport.getAbsolutePath();
    if (!findbugsXmlReport.exists()) {
      throw new IllegalStateException("The findbugs XML report can't be found at '" + findbugsXmlReportPath + "'");
    }
  }

  public List<XmlBugInstance> getBugInstances() {
    List<XmlBugInstance> result = new ArrayList<>();
    try {
      SMInputFactory inf = new SMInputFactory(XMLInputFactory.newInstance());
      SMInputCursor cursor = inf.rootElementCursor(findbugsXmlReport).advance();
      SMInputCursor bugInstanceCursor = cursor.childElementCursor("BugInstance").advance();
      while (bugInstanceCursor.asEvent() != null) {
        XmlBugInstance xmlBugInstance = new XmlBugInstance();
        xmlBugInstance.type = bugInstanceCursor.getAttrValue("type");
        xmlBugInstance.longMessage = "";
        result.add(xmlBugInstance);

        List<XmlSourceLineAnnotation> lines = new ArrayList<>();
        SMInputCursor bugInstanceChildCursor = bugInstanceCursor.childElementCursor().advance();
        while (bugInstanceChildCursor.asEvent() != null) {
          String nodeName = bugInstanceChildCursor.getLocalName();
          if ("LongMessage".equals(nodeName)) {
            xmlBugInstance.longMessage = bugInstanceChildCursor.collectDescendantText();
          } else if ("SourceLine".equals(nodeName)) {
            XmlSourceLineAnnotation xmlSourceLineAnnotation = new XmlSourceLineAnnotation();
            xmlSourceLineAnnotation.parseStart(bugInstanceChildCursor.getAttrValue("start"));
            xmlSourceLineAnnotation.parseEnd(bugInstanceChildCursor.getAttrValue("end"));
            xmlSourceLineAnnotation.parsePrimary(bugInstanceChildCursor.getAttrValue("primary"));
            xmlSourceLineAnnotation.className = bugInstanceChildCursor.getAttrValue("classname");
            lines.add(xmlSourceLineAnnotation);
          }
          bugInstanceChildCursor.advance();
        }
        xmlBugInstance.sourceLines = Collections.unmodifiableList(lines);
        bugInstanceCursor.advance();
      }
      cursor.getStreamReader().closeCompletely();
    } catch (XMLStreamException e) {
      throw new IllegalStateException("Unable to parse the Findbugs XML Report '" + findbugsXmlReportPath + "'", e);
    }
    return result;
  }

  public static class XmlBugInstance {
    private String type;
    private String longMessage;
    private List<XmlSourceLineAnnotation> sourceLines;

    public String getType() {
      return type;
    }

    public String getLongMessage() {
      return longMessage;
    }

    @CheckForNull
    public XmlSourceLineAnnotation getPrimarySourceLine() {
      for (XmlSourceLineAnnotation sourceLine : sourceLines) {
        if (sourceLine.isPrimary()) {
          // According to source code of Findbugs 2.0 - should be exactly one primary
          return sourceLine;
        }
      }
      // As a last resort - return first line
      return sourceLines.isEmpty() ? null : sourceLines.get(0);
    }

  }

  public static class XmlSourceLineAnnotation {
    private boolean primary;
    private Integer start;
    private Integer end;
    protected String className;

    public void parseStart(String attrValue) {
      try {
        start = Integer.valueOf(attrValue);
      } catch (NumberFormatException e) {
        start = null;
      }
    }

    public void parseEnd(String attrValue) {
      try {
        end = Integer.valueOf(attrValue);
      } catch (NumberFormatException e) {
        end = null;
      }
    }

    public void parsePrimary(String attrValue) {
      primary = Boolean.parseBoolean(attrValue);
    }

    public boolean isPrimary() {
      return primary;
    }

    public Integer getStart() {
      return start;
    }

    public Integer getEnd() {
      return end;
    }

    public String getClassName() {
      return className;
    }

    public String getSonarJavaFileKey() {
      if (className.indexOf('$') > -1) {
        return className.substring(0, className.indexOf('$'));
      }
      return className;
    }

  }

}
