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
genesDone = new java.util.HashSet();
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
database.setInfo("DATATYPE", "Nanoamterial");

def addXRef(GdbConstruct database, Xref ref, String node, DataSource source, Set identifiersDone, Set linkesDone, boolean isPrimary) {
   id = node.trim()
   if (id.length() > 0) {
     // println "id($source): $id"
     ref2 = new Xref(id, source, isPrimary);
     if (!genesDone.contains(ref2.toString())) {
       if (database.addGene(ref2) != 0) {
          println "Error (addXRef.addGene): " + database.recentException().getMessage()
          println "                 id($source): $id"
       }
       genesDone.add(ref2.toString())
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

database.commit();
database.finalize();
