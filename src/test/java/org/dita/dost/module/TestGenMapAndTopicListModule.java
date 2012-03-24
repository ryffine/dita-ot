/*
 * This file is part of the DITA Open Toolkit project hosted on
 * Sourceforge.net. See the accompanying license.txt file for
 * applicable licenses.
 */

/*
 * (c) Copyright IBM Corp. 2010 All Rights Reserved.
 */
package org.dita.dost.module;

import static org.junit.Assert.*;
import static org.dita.dost.util.Constants.*;
import static org.dita.dost.util.Job.*;
import static org.dita.dost.module.GenMapAndTopicListModule.KeyDef;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.dita.dost.TestUtils;
import org.dita.dost.exception.DITAOTException;
import org.dita.dost.pipeline.AbstractFacade;
import org.dita.dost.pipeline.PipelineFacade;
import org.dita.dost.pipeline.PipelineHashIO;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class TestGenMapAndTopicListModule {

    private static final File resourceDir = new File("test-stub", TestGenMapAndTopicListModule.class.getSimpleName());
    private static final File srcDir = new File(resourceDir, "src");
    private static final File expDir = new File(resourceDir, "exp");
    
    private static File tempDir;
    private static File tempDirParallel;
    private static File tempDirAbove;

    @BeforeClass
    public static void setUp() throws IOException, DITAOTException {
        tempDir = TestUtils.createTempDir(TestGenMapAndTopicListModule.class);
        
        tempDirParallel = new File(tempDir, "parallel");
        tempDirParallel.mkdirs();
        final File inputDirParallel = new File("maps");
        final File inputMapParallel = new File(inputDirParallel, "root-map-01.ditamap");
        final File outDirParallel = new File(tempDirParallel, "out");
        generate(inputDirParallel, inputMapParallel, outDirParallel, tempDirParallel);
        
        tempDirAbove = new File(tempDir, "above");
        tempDirAbove.mkdirs();
        final File inputDirAbove = new File(".");
        final File inputMapAbove = new File(inputDirAbove, "root-map-02.ditamap");
        final File outDirAbove = new File(tempDirAbove, "out");
        generate(inputDirAbove, inputMapAbove, outDirAbove, tempDirAbove);
    }

    private static void generate(final File inputDir, final File inputMap, final File outDir, final File tempDir) throws DITAOTException {
        final PipelineHashIO pipelineInput = new PipelineHashIO();
        pipelineInput.setAttribute(ANT_INVOKER_PARAM_INPUTMAP, inputMap.getPath());
        pipelineInput.setAttribute(ANT_INVOKER_PARAM_BASEDIR, srcDir.getAbsolutePath());
        pipelineInput.setAttribute(ANT_INVOKER_EXT_PARAM_DITADIR, inputDir.getPath());
        pipelineInput.setAttribute(ANT_INVOKER_EXT_PARAM_OUTPUTDIR, outDir.getPath());
        pipelineInput.setAttribute(ANT_INVOKER_PARAM_TEMPDIR, tempDir.getPath());
        pipelineInput.setAttribute(ANT_INVOKER_EXT_PARAM_DITADIR, new File(".").getAbsolutePath());
        pipelineInput.setAttribute(ANT_INVOKER_PARAM_DITAEXT, ".xml");
        pipelineInput.setAttribute(ANT_INVOKER_EXT_PARAM_INDEXTYPE, "xhtml");
        pipelineInput.setAttribute(ANT_INVOKER_EXT_PARAM_ENCODING, "en-US");
        pipelineInput.setAttribute(ANT_INVOKER_EXT_PARAM_TARGETEXT, ".html");
        pipelineInput.setAttribute(ANT_INVOKER_EXT_PARAM_VALIDATE, Boolean.TRUE.toString());
        pipelineInput.setAttribute(ANT_INVOKER_EXT_PARAM_GENERATECOPYOUTTER, "1");
        pipelineInput.setAttribute(ANT_INVOKER_EXT_PARAM_OUTTERCONTROL, "warn");
        pipelineInput.setAttribute(ANT_INVOKER_EXT_PARAM_ONLYTOPICINMAP, "false");
        //pipelineInput.setAttribute("ditalist", new File(tempDir, FILE_NAME_DITA_LIST).getPath());
        pipelineInput.setAttribute(ANT_INVOKER_PARAM_MAPLINKS, new File(tempDir, "maplinks.unordered").getPath());
        pipelineInput.setAttribute(ANT_INVOKER_EXT_PARAN_SETSYSTEMID, "no");

        final AbstractFacade facade = new PipelineFacade();
        facade.setLogger(new TestUtils.TestLogger());
        facade.execute("GenMapAndTopicList", pipelineInput);
    }
        
    @Test
    public void testFileContentParallel() throws Exception{
        testFileContent(new File(expDir, "parallel"), tempDirParallel);

        final Properties ditaProps = readProperties(new File(tempDirParallel, FILE_NAME_DITA_LIST));
        assertEquals(".." + UNIX_SEPARATOR, ditaProps.getProperty("uplevels"));

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final Document document = builder.parse(new File(tempDirParallel+ File.separator + KEYDEF_LIST_FILE));
        final Element elem = document.getDocumentElement();
        final NodeList nodeList = elem.getElementsByTagName("keydef");
        final Map<String, List<String>> expKeyDef = new HashMap<String, List<String>>();
        expKeyDef.put("target_topic_2", Arrays.asList("target_topic_2", "topics" + UNIX_SEPARATOR + "target-topic-c.xml", "maps" + UNIX_SEPARATOR + "root-map-01.ditamap"));
        expKeyDef.put("target_topic_1", Arrays.asList("target_topic_1", "topics" + UNIX_SEPARATOR + "target-topic a.xml", "maps" + UNIX_SEPARATOR + "root-map-01.ditamap"));
        expKeyDef.put("target_topic_3", Arrays.asList("target_topic_3", "topics" + UNIX_SEPARATOR + "target-topic-c.xml", "maps" + UNIX_SEPARATOR + "root-map-01.ditamap"));
        expKeyDef.put("target_topic_4", Arrays.asList("target_topic_4", "http://www.example.com/?foo=bar&baz=qux#quxx", "maps" + UNIX_SEPARATOR + "root-map-01.ditamap"));
        for(int i = 0; i< nodeList.getLength();i++){
            final Element e = (Element) nodeList.item(i);
            final List<String> exp = expKeyDef.get(e.getAttribute("keys"));
            assertEquals(exp.get(0), e.getAttribute("keys"));
            assertEquals(exp.get(1), e.getAttribute("href"));
            assertEquals(exp.get(2), e.getAttribute("source"));
        }
    }
    
    @Test
    public void testFileContentAbove() throws Exception{
        testFileContent(new File(expDir, "above"), tempDirAbove);

        final Properties ditaProps = readProperties(new File(tempDirAbove, FILE_NAME_DITA_LIST));
        assertEquals("", ditaProps.getProperty("uplevels"));
        
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final Document document = builder.parse(new File(tempDirAbove+ File.separator + KEYDEF_LIST_FILE));
        final Element elem = document.getDocumentElement();
        final NodeList nodeList = elem.getElementsByTagName("keydef");
        final Map<String, List<String>> expKeyDef = new HashMap<String, List<String>>();
        expKeyDef.put("target_topic_2", Arrays.asList("target_topic_2", "topics" + UNIX_SEPARATOR + "target-topic-c.xml", "root-map-02.ditamap"));
        expKeyDef.put("target_topic_1", Arrays.asList("target_topic_1", "topics" + UNIX_SEPARATOR + "target-topic a.xml", "root-map-02.ditamap"));
        expKeyDef.put("target_topic_3", Arrays.asList("target_topic_3", "topics" + UNIX_SEPARATOR + "target-topic-c.xml", "root-map-02.ditamap"));
        expKeyDef.put("target_topic_4", Arrays.asList("target_topic_4", "http://www.example.com/?foo=bar&baz=qux#quxx", "root-map-02.ditamap"));
        for(int i = 0; i< nodeList.getLength();i++){
            final Element e = (Element) nodeList.item(i);
            final List<String> exp = expKeyDef.get(e.getAttribute("keys"));
            assertEquals(exp.get(0), e.getAttribute("keys"));
            assertEquals(exp.get(1), e.getAttribute("href"));
            assertEquals(exp.get(2), e.getAttribute("source"));
        }
    }
    
    @Test
    public void testKeyDefStringStringString() {
        final KeyDef k = new KeyDef("foo", "bar", "baz");
        assertEquals("foo", k.keys);
        assertEquals("bar", k.href);
        assertEquals("baz", k.source);
        final KeyDef n = new KeyDef("foo", null, null);
        assertEquals("foo", n.keys);
        assertNull(n.href);
        assertNull(n.source);
    }
    
    @Test
    public void testKeyDefString() {
        final KeyDef k = new KeyDef("foo=bar(baz)");
        assertEquals("foo", k.keys);
        assertEquals("bar", k.href);
        assertEquals("baz", k.source);
        final KeyDef n = new KeyDef("foo=");
        assertEquals("foo", n.keys);
        assertNull(n.href);
        assertNull(n.source);
    }
    @Test
    public void testKeyDefToString() {
        final KeyDef k = new KeyDef("foo", "bar", "baz");
        assertEquals("foo=bar(baz)", k.toString());
        final KeyDef n = new KeyDef("foo", null, null);
        assertEquals("foo=", n.toString());
    }
    
    private void testFileContent(final File expDir, final File actDir) throws Exception{
        for (final File f: expDir.listFiles()) {
            if (f.getName().equals(FILE_NAME_DITA_LIST)) {
                continue;
            } else if (f.getName().equals(KEY_LIST_FILE)) {
                assertEquals("Comparing " + f.getName(), readProperties(f), readProperties(new File(actDir, f.getName())));
            } else if (f.getName().endsWith(".list")) {
                assertEquals("Comparing " + f.getName(), readLines(f), readLines(new File(actDir, f.getName())));
            } else {
                assertTrue(new File(actDir, f.getName()).exists());
            }
        }
    }
    
    private Properties readProperties(final File f)
            throws IOException, FileNotFoundException {
        final Properties p = new Properties();
        InputStream in = null;
        try {
            in = new FileInputStream(f);
            p.load(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return p;
    }
    
    private Set<String> readLines(final File f) throws IOException {
        final Set<String> lines = new HashSet<String>();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(f));
            String line = null;
            while ((line = in.readLine()) != null) {
                lines.add(line);
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return lines;
    }

    @AfterClass
    public static void tearDown() throws IOException {
        TestUtils.forceDelete(tempDir);
    }

}