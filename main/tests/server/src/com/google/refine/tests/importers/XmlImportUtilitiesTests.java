package com.google.refine.tests.importers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.importers.TreeImportUtilities.ImportColumn;
import com.google.refine.importers.TreeImportUtilities.ImportColumnGroup;
import com.google.refine.importers.TreeImportUtilities.ImportRecord;
import com.google.refine.importers.parsers.JSONParser;
import com.google.refine.importers.parsers.TreeParser;
import com.google.refine.importers.parsers.TreeParserToken;
import com.google.refine.importers.parsers.XmlParser;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.tests.RefineTest;


public class XmlImportUtilitiesTests extends RefineTest {

    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    //dependencies
    Project project;
    TreeParser parser;
    ImportColumnGroup columnGroup;
    ImportRecord record;
    ByteArrayInputStream inputStream;

    //System Under Test
    XmlImportUtilitiesStub SUT;

    @BeforeMethod
    public void SetUp(){
        SUT = new XmlImportUtilitiesStub();
        project = new Project();
        columnGroup = new ImportColumnGroup();
        record = new ImportRecord();
    }

    @AfterMethod
    public void TearDown() throws IOException{
        SUT = null;
        project = null;
        parser = null;
        columnGroup = null;
        record = null;
        if(inputStream != null)
           inputStream.close();
        inputStream = null;
    }

    @Test
    public void detectPathFromTagXmlTest(){
        loadData("<?xml version=\"1.0\"?><library><book id=\"1\"><author>author1</author><genre>genre1</genre></book></library>");

        String tag = "library";
        createXmlParser();

        String[] response = XmlImportUtilitiesStub.detectPathFromTag(parser, tag);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.length, 1);
        Assert.assertEquals(response[0], "library");
    }

    @Test
    public void detectPathFromTagWithNestedElementXml(){
        loadData("<?xml version=\"1.0\"?><library><book id=\"1\"><author>author1</author><genre>genre1</genre></book></library>");
        String tag = "book";

        createXmlParser();

        String[] response = XmlImportUtilitiesStub.detectPathFromTag(parser, tag);
        Assert.assertNotNull(response);
        Assert.assertEquals(response.length, 2);
        Assert.assertEquals(response[0], "library");
        Assert.assertEquals(response[1], "book");
    }

    @Test
    public void detectRecordElementXmlTest(){
        loadData("<?xml version=\"1.0\"?><library><book id=\"1\"><author>author1</author><genre>genre1</genre></book></library>");
        createXmlParser();

        String tag="library";

        List<String> response = new ArrayList<String>();
        try {
            response = SUT.detectRecordElementWrapper(parser, tag);
        } catch (ServletException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertNotNull(response);
        Assert.assertEquals(response.size(), 1);
        Assert.assertEquals(response.get(0), "library");
    }

    @Test
    public void detectRecordElementCanHandleWithNestedElementsXml(){
        loadData("<?xml version=\"1.0\"?><library><book id=\"1\"><author>author1</author><genre>genre1</genre></book></library>");
        createXmlParser();

        String tag="book";

        List<String> response = new ArrayList<String>();
        try {
            response = SUT.detectRecordElementWrapper(parser, tag);
        } catch (ServletException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertNotNull(response);
        Assert.assertEquals(response.size(), 2);
        Assert.assertEquals(response.get(0), "library");
        Assert.assertEquals(response.get(1), "book");
    }

    @Test
    public void detectRecordElementIsNullForUnfoundTagXml(){
        loadData("<?xml version=\"1.0\"?><library><book id=\"1\"><author>author1</author><genre>genre1</genre></book></library>");
        createXmlParser();

        String tag="";

        List<String> response = new ArrayList<String>();
        try {
            response = SUT.detectRecordElementWrapper(parser, tag);
        } catch (ServletException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertNull(response);
    }

    @Test
    public void detectRecordElementRegressionXmlTest(){
        loadSampleXml();

        String[] path = XmlImportUtilitiesStub.detectRecordElement(new XmlParser(inputStream));
        Assert.assertNotNull(path);
        Assert.assertEquals(path.length, 2);
        Assert.assertEquals(path[0], "library");
        Assert.assertEquals(path[1], "book");
    }
    
    @Test
    public void detectRecordElementRegressionJsonTest(){
        loadSampleJson();

        String[] path = XmlImportUtilitiesStub.detectRecordElement(new JSONParser(inputStream));
        Assert.assertNotNull(path);
        Assert.assertEquals(path.length, 2);
        Assert.assertEquals(path[0], "__anonymous__");
        Assert.assertEquals(path[1], "__anonymous__");
    }

    @Test
    public void importTreeDataXmlTest(){
        loadSampleXml();

        String[] recordPath = new String[]{"library","book"};
        XmlImportUtilitiesStub.importTreeData(new XmlParser(inputStream), project, recordPath, columnGroup );

        log(project);
        assertProjectCreated(project, 0, 6);

        Assert.assertEquals(project.rows.get(0).cells.size(), 5);

        Assert.assertEquals(columnGroup.subgroups.size(), 1);
        Assert.assertNotNull(columnGroup.subgroups.get("book"));
        Assert.assertEquals(columnGroup.subgroups.get("book").subgroups.size(), 3);
        Assert.assertNotNull(columnGroup.subgroups.get("book").subgroups.get("author"));
        Assert.assertNotNull(columnGroup.subgroups.get("book").subgroups.get("title"));
        Assert.assertNotNull(columnGroup.subgroups.get("book").subgroups.get("publish_date"));
    }

    @Test
    public void importXmlWithVaryingStructureTest(){
        loadData(XmlImporterTests.getSampleWithVaryingStructure());

        String[] recordPath = new String[]{"library", "book"};
        XmlImportUtilitiesStub.importTreeData(new XmlParser(inputStream), project, recordPath, columnGroup);

        log(project);
        assertProjectCreated(project, 0, 6);
        Assert.assertEquals(project.rows.get(0).cells.size(), 5);
        Assert.assertEquals(project.rows.get(5).cells.size(), 6);

        Assert.assertEquals(columnGroup.subgroups.size(), 1);
        Assert.assertEquals(columnGroup.name, "");
        ImportColumnGroup book = columnGroup.subgroups.get("book");
        Assert.assertNotNull(book);
        Assert.assertEquals(book.columns.size(), 1);
        Assert.assertEquals(book.subgroups.size(), 4);
        Assert.assertNotNull(book.subgroups.get("author"));
        Assert.assertEquals(book.subgroups.get("author").columns.size(), 1);
        Assert.assertNotNull(book.subgroups.get("title"));
        Assert.assertNotNull(book.subgroups.get("publish_date"));
        Assert.assertNotNull(book.subgroups.get("genre"));
    }

    @Test
    public void createColumnsFromImportTest(){

        ImportColumnGroup columnGroup = new ImportColumnGroup();
        ImportColumnGroup subGroup = new ImportColumnGroup();
        columnGroup.columns.put("a", new ImportColumn("hello"));
        columnGroup.columns.put("b", new ImportColumn("world"));
        subGroup.columns.put("c", new ImportColumn("foo"));
        subGroup.columns.put("d", new ImportColumn("bar"));
        columnGroup.subgroups.put("e", subGroup);

        XmlImportUtilitiesStub.createColumnsFromImport(project, columnGroup);
        log(project);
        assertProjectCreated(project, 4, 0);
        Assert.assertEquals(project.columnModel.columns.get(0).getName(), "world");
        Assert.assertEquals(project.columnModel.columns.get(1).getName(), "hello");
        Assert.assertEquals(project.columnModel.columns.get(2).getName(), "bar");
        Assert.assertEquals(project.columnModel.columns.get(3).getName(), "foo");
        Assert.assertEquals(project.columnModel.columnGroups.get(0).keyColumnIndex, 2);
        Assert.assertEquals(project.columnModel.columnGroups.get(0).startColumnIndex, 2);
        Assert.assertEquals(project.columnModel.columnGroups.get(0).columnSpan, 2);
    }

    @Test
    public void findRecordTestXml(){
        loadSampleXml();
        createXmlParser();
        ParserSkip();

        String[] recordPath = new String[]{"library","book"};
        int pathIndex = 0;

        try {
            SUT.findRecordWrapper(project, parser, recordPath, pathIndex, columnGroup);
        } catch (ServletException e) {
            Assert.fail();
        }

        log(project);
        assertProjectCreated(project, 0, 6);

        Assert.assertEquals(project.rows.get(0).cells.size(), 5);
        //TODO
    }

    @Test
    public void processRecordTestXml(){
        loadData("<?xml version=\"1.0\"?><library><book id=\"1\"><author>author1</author><genre>genre1</genre></book></library>");
        createXmlParser();
        ParserSkip();

        try {
            SUT.processRecordWrapper(project, parser, columnGroup);
        } catch (ServletException e) {
            Assert.fail();
        }
        log(project);
        Assert.assertNotNull(project.rows);
        Assert.assertEquals(project.rows.size(), 1);
        Row row = project.rows.get(0);
        Assert.assertNotNull(row);
        Assert.assertNotNull(row.getCell(2));
        Assert.assertEquals(row.getCell(2).value, "author1");

    }

    @Test
    public void processRecordTestDuplicateColumnsXml(){
        loadData("<?xml version=\"1.0\"?><library><book id=\"1\"><authors><author>author1</author><author>author2</author></authors><genre>genre1</genre></book></library>");
        createXmlParser();
        ParserSkip();

        try {
            SUT.processRecordWrapper(project, parser, columnGroup);
        } catch (ServletException e) {
            Assert.fail();
        }
        log(project);
        Assert.assertNotNull(project.rows);
        Assert.assertEquals(project.rows.size(), 2);

        Row row = project.rows.get(0);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 4);
        Assert.assertNotNull(row.getCell(2));
        Assert.assertEquals(row.getCell(2).value, "author1");

        row = project.rows.get(1);
        Assert.assertEquals(row.getCell(2).value, "author2");
    }

    @Test
    public void processRecordTestNestedElementXml(){
        loadData("<?xml version=\"1.0\"?><library><book id=\"1\"><author><author-name>author1</author-name><author-dob>a date</author-dob></author><genre>genre1</genre></book></library>");
        createXmlParser();
        ParserSkip();

        try {
            SUT.processRecordWrapper(project, parser, columnGroup);
        } catch (ServletException e) {
            Assert.fail();
        }
        log(project);
        Assert.assertNotNull(project.rows);
        Assert.assertEquals(project.rows.size(), 1);
        Row row = project.rows.get(0);
        Assert.assertNotNull(row);
        Assert.assertEquals(row.cells.size(), 5);
        Assert.assertNotNull(row.getCell(2));
        Assert.assertEquals(row.getCell(2).value, "author1");
        Assert.assertNotNull(row.getCell(3));
        Assert.assertEquals(row.getCell(3).value, "a date");
    }


    @Test
    public void processSubRecordTestXml(){
        loadData("<?xml version=\"1.0\"?><library><book id=\"1\"><author>author1</author><genre>genre1</genre></book></library>");
        createXmlParser();
        ParserSkip();

        try {
            SUT.ProcessSubRecordWrapper(project, parser, columnGroup, record);
        } catch (ServletException e) {
            Assert.fail();
        }
        log(project);

        Assert.assertEquals(columnGroup.subgroups.size(), 1);
        Assert.assertEquals(columnGroup.name, "");

        Assert.assertNotNull(columnGroup.subgroups.get("library"));
        Assert.assertEquals(columnGroup.subgroups.get("library").subgroups.size(), 1);

        ImportColumnGroup book = columnGroup.subgroups.get("library").subgroups.get("book");
        Assert.assertNotNull(book);
        Assert.assertEquals(book.subgroups.size(), 2);
        Assert.assertNotNull(book.subgroups.get("author"));
        Assert.assertNotNull(book.subgroups.get("genre"));

        //TODO check record
    }

    @Test
    public void addCellTest(){
        String columnLocalName = "author";
        String text = "Author1, The";
        int commonStartingRowIndex = 0;
        SUT.addCellWrapper(project, columnGroup, record, columnLocalName, text, commonStartingRowIndex);

        Assert.assertNotNull(record);
        Assert.assertNotNull(record.rows);
        //Assert.assertNotNull(record.columnEmptyRowIndices);
        Assert.assertEquals(record.rows.size(), 1);
        //Assert.assertEquals(record.columnEmptyRowIndices.size(), 2);
        Assert.assertNotNull(record.rows.get(0));
        //Assert.assertNotNull(record.columnEmptyRowIndices.get(0));
        //Assert.assertNotNull(record.columnEmptyRowIndices.get(1));
        Assert.assertEquals(record.rows.get(0).size(), 2);
        Assert.assertNotNull(record.rows.get(0).get(1));
        Assert.assertEquals(record.rows.get(0).get(1).value, "Author1, The");
        //Assert.assertEquals(record.columnEmptyRowIndices.get(0).intValue(),0);
        //Assert.assertEquals(record.columnEmptyRowIndices.get(1).intValue(),1);

    }

    //----------------helpers-------------
    public void loadSampleXml(){
        loadData( XmlImporterTests.getSample() );
    }
    
    public void loadSampleJson(){
        loadData( JsonImporterTests.getSample() );
    }

    public void loadData(String xml){
        try {
            inputStream = new ByteArrayInputStream( xml.getBytes( "UTF-8" ) );
        } catch (UnsupportedEncodingException e1) {
            Assert.fail();
        }
    }

    public void ParserSkip(){
        try {
            if(parser.getEventType() == TreeParserToken.Ignorable){
                parser.next(); //move parser forward once e.g. skip the START_DOCUMENT parser event
            }
        } catch (ServletException e1) {
            Assert.fail();
        }
    }

    public void createXmlParser(){
        parser = new XmlParser(inputStream);
    }
    public void createJsonParser(){
        parser = new JSONParser(inputStream);
    }
}
