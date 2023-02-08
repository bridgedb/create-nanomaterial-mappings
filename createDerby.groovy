@Grab(group='org.bridgedb', module='org.bridgedb', version='3.0.18')
@Grab(group='org.bridgedb', module='org.bridgedb.bio', version='3.0.18')
@Grab(group='org.bridgedb', module='org.bridgedb.rdb', version='3.0.18')
@Grab(group='org.bridgedb', module='org.bridgedb.rdb.construct', version='3.0.18')

import java.text.SimpleDateFormat;
import java.util.Date;
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.NodeChildren;

import org.bridgedb.IDMapperException;
import org.bridgedb.DataSource;
import org.bridgedb.Xref;
import org.bridgedb.bio.DataSourceTxt;
import org.bridgedb.rdb.construct.DBConnector;
import org.bridgedb.rdb.construct.DataDerby;
import org.bridgedb.rdb.construct.GdbConstruct;
import org.bridgedb.rdb.construct.GdbConstructImpl4;

commitInterval = 500
identifiersDone = new java.util.HashSet();
linksDone = new java.util.HashSet();

DataSourceTxt.init()

GdbConstruct database = GdbConstructImpl4.createInstance(
  "nanomaterials", new DataDerby(), DBConnector.PROP_RECREATE
);
database.createGdbTables();
database.preInsert();

String dateStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
database.setInfo("BUILDDATE", dateStr);
database.setInfo("DATASOURCENAME", "create-nanomaterial-mappings");
database.setInfo("DATASOURCEVERSION", dateStr);
database.setInfo("DATATYPE", "nanomaterial");
database.setInfo("SERIES", "nanomaterials");

ermDS = DataSource.register("Nmerm", "European Registry of Materials").asDataSource()
nanomileDS = DataSource.register("Nmnm", "NanoMile").asDataSource()

def addXRef(GdbConstruct database, Xref ref, String node, DataSource source, Set identifiersDone, Set linkesDone, boolean isPrimary) {
   id = node.trim()
   if (id.length() > 0) {
     // println "id($source): $id"
     ref2 = new Xref(id, source, isPrimary);
     if (!identifiersDone.contains(ref2.toString())) {
       if (database.addGene(ref2) != 0) {
          println "Error (addXRef.addGene): " + database.recentException().getMessage()
          println "                 id($source): $id"
       }
       identifiersDone.add(ref2.toString())
     }
     if (!linksDone.contains(ref.toString()+ref2.toString())) {
       if (database.addLink(ref, ref2) != 0) {
         println "Error (addXRef.addLink): " + database.recentException().getMessage()
         println "                 id(origin):  " + ref.toString()
         println "                 id($source): $id"
       }
       linksDone.add(ref.toString()+ref2.toString())
     }
   }
}

def addXRef(GdbConstruct database, Xref ref, String node, DataSource source, Set identifiersDone, Set linkesDone) {
  addXRef(database, ref, node, source, identifiersDone, linkesDone, (boolean)true)
}

// NanoMILE
counter = 0
error = 0
new File("nanomile.csv").eachLine { line,number ->
  if (line.trim().startsWith("#")) return; // skip comment lines
  fields = line.split(",")
  rootid = fields[0]
  Xref ref = new Xref(rootid, nanomileDS);
  if (!identifiersDone.contains(ref.toString())) {
    addError = database.addGene(ref);
    if (addError != 0) println "Error (addGene): " + database.recentException().getMessage()
    error += addError
    linkError = database.addLink(ref,ref);
    if (linkError != 0) println "Error (addLinkItself): " + database.recentException().getMessage()
    error += linkError
    identifiersDone.add(ref.toString())
  }

  // add external identifiers
  ermID = fields[1].replaceAll("erm:","")
  println ermID
  addXRef(database, ref, ermID, ermDS, identifiersDone, linksDone);

  counter++
  if (counter % commitInterval == 0) {
    println "Info: errors: " + error + " (PubChem)"
    database.commit()
  }
}

database.commit();
database.finalize();
