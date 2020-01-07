/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.sakilaclient;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author Caroline
 */
//REST Client
public class SakilaClient extends javax.swing.JFrame {
   
   //Grundsökvägen till RESTful servicen jag vill anropa
   private static final String BASE_URI = "http://localhost:8080/SakilaWS/sakila/actors";
   //2D array för data til JTable (Tablemodel)
   private Object[][] data;
   private DefaultTableModel tblModel;

   /**
    * Creates new form SakilaClient
    */
   public SakilaClient() {
      initComponents();
   }
   
//Relaterar arrayen data till JTable(tblActors) som visar upp innehållet   
//aka initTable() i GuiDbDemo.java i D0024E
   private void loadDataToTable() {
      
      String[] columnNames = {"ID", "Förnamn", "Efternamn"};
      tblModel = new DefaultTableModel(this.data, columnNames);
      tblActors.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      tblActors.setModel(tblModel);
      tblActors.getColumnModel().getColumn(0).setPreferredWidth(5);
      tblActors.getColumnModel().getColumn(1).setPreferredWidth(10);
      tblActors.getColumnModel().getColumn(2).setPreferredWidth(10);
      tblActors.setShowGrid(true);
   }
   
   private void getActorsByURL() {
      
      try {
         //Använd klassen URL för att peka ut en resurs på WWW
         //https://docs.oracle.com/javase/8/docs/api/java/net/URL.html
         URL url = new URL(SakilaClient.BASE_URI);
         //Öppnar connectionen mot REST servicen
         HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
         httpCon.setRequestMethod("GET");
         httpCon.setRequestProperty("Accept", "application/json");
         
         //Generera undantag om connection
         if (httpCon.getResponseCode() != 200) {
            throw new RuntimeException("FEL : HTTP-kod : " + 
                    httpCon.getResponseCode());
         }
         
         //getInputstream() anropar angivet http-REST-API och tar emot responsen som en teckenström
         BufferedReader br = new BufferedReader(new InputStreamReader(
                 httpCon.getInputStream()));
         
         //Läs av rad för rad från BufferedReadern till min tabell
         String line = br.readLine();
         while (line != null) {
            //Testa först med en System.out.println så jag ser att jag får ut data och vilken sorts data det är, 
            //så jag kan skapa en egen klass som matchar och kan hålla datat.
//            System.out.println(line);
            //Ladda in datan i min tabell
            loadJTableFromJson(line);
            line = br.readLine();
         }
         
         //Ett annat sätt at läsa av alla rader från BufferedReadern till min tabell
         //loadJTableFromJson(br.lines().collect(Collectors.joining(System.lineSeparator())));
         
         //Signalera stängning av http-connection
         httpCon.disconnect();
         
      } catch (MalformedURLException urlEx) {
         Logger.getLogger(SakilaClient.class.getName()).log(Level.SEVERE, null, urlEx);
      } catch (IOException ioEx) {
         Logger.getLogger(SakilaClient.class.getName()).log(Level.SEVERE, null, ioEx);
      }
   }
   
   private void loadJTableFromJson(String jsonInput) {
      //Konvertera JSON-Array till en Array med Java-objekt (Av en klass som jag skapat som matchar)
      //Finns flera olika 3e-parts-bibliotek som kan användas för detta på https://www.json.org/json-en.html
      //I detta fallet används google-gson
      Person[] personArray = new Gson().fromJson(jsonInput, Person[].class);
      
      //Specifiera storlek på en annan array (kallad data) som används för att föra över datat till min JTable sen.
      int rows = personArray.length;
      data = new Object[rows][3];
      int row = 0;
      for(Person person : personArray) {
         //test
//         System.out.println(person.getId() + person.getFirst_name() + "" + person.getLast_name());
         //Ladda JTable
         data[row][0] = person.getId();
         data[row][1] = person.getFirst_name();
         data[row][2] = person.getLast_name();
         row++;
      }
      loadDataToTable(); //aka initTable() i GuiDbDemo.java i D0024E
   }
   
   //Dåligt att lägga till / ändra data via get. Get är endast lämplig för att hämta data.
   //Deta enbart exempel för att demonstrera hur man kan arbeta med parametrar.
   private void addNewByGet() {
      //JAX-RS Client - Ett rekomenderat sätt att koppla upp sig (framför URL)
      //https://howtodoinjava.com/jersey/jersey-restful-client-examples/
      //jersey-client dependencyn behöver även jersey-hk2 dependencyn av samma version för att funka.
      Client client = ClientBuilder.newClient();
      //tar min bas-uri och lägger på rätt path
      WebTarget target = client.target(SakilaClient.BASE_URI).path("/actor");
      //Skapa en anropsbyggare genom att använda target som håller URI...
      Invocation.Builder invocationBuilder = target
              //...lägga på önskade queryParametrar...
              .queryParam("first", txtFName.getText())
              .queryParam("last", txtLName.getText())
              //... börja bygga en request och ange vilken mediatyp som accepteras som respons.
              .request().accept(MediaType.APPLICATION_JSON); //datatyp för överföringen
      //Kör anropet med angiven metod, i detta fallet GET. Kan även vara .put(), .post() eller .delete()
      Response r = invocationBuilder.get();
      if (r.getStatus() != 201) {
         JOptionPane.showMessageDialog(this, "FEL: " + r.getStatus());
      } else {
         getActorsByURL();//Updatera tabellen för att även visa senaste
         //Visa sökvägen till den nyligt skpade posten
         JOptionPane.showMessageDialog(this, "Ny post lades till: " + r.getLocation());
      }
   }
   
   //Lägger till ny actor i databasen mha ett objekt
   private void addNewByPost() {
      //JAX-RS Client - Ett rekomenderat sätt att koppla upp sig (framför URL)
      //https://howtodoinjava.com/jersey/jersey-restful-client-examples/
      //jersey-client dependencyn behöver även jersey-hk2 dependencyn av samma version för att funka.
      Client client = ClientBuilder.newClient();
      //Går att göra om Client och WebTarget så de går at återanvända
      //istället för att skapa dem i varje metod - Görs så nu för exemplets skull.
      WebTarget target = client.target(SakilaClient.BASE_URI);
      
      //Klass som matchar det objekt som ws-metoden vill ha (dvs samma instansvariabler med exakt samma namn)
      Person person = new Person();
      person.setFirst_name(txtFName.getText());
      person.setLast_name(txtLName.getText());
      
      //Skapa en anropsbyggare genom att använda target som håller URI...
      //... börja bygga en request och ange samtidigt vilken mediatyp som accepteras som respons.
      Invocation.Builder invocationBuilder = target.request(MediaType.APPLICATION_JSON);
      //Kör anropet med angivn metod, i detta fallet POST, 
      //och skickar med objektet i bodyn i form av en entitet av den angivna Mediatypen. 
      //Kan även vara .put(), .get() eller .delete()
      //OBS! För att skicka ett JSON-objekt i bodyn med anropet behövs jersey-media-moxy dependencyn av samma version för att funka. (Eller annan motsvarande bibliotek som stödjer json-hantering i bodyn.)
      Response r = invocationBuilder.post(Entity.entity(person, MediaType.APPLICATION_JSON_TYPE));
      if (r.getStatus() != 201) {
         JOptionPane.showMessageDialog(this, "FEL: " + r.getStatus());
      } else {
         getActorsByURL();//Updatera tabellen för att även visa senaste
         //Visa sökvägen till den nyligt skpade posten
         JOptionPane.showMessageDialog(this, "Ny post lades till: " + r.getLocation());
      }
   }
   
   //Lägger till ny actor i databasen mha data från webformulär
   private void addNewByForm() {
      //JAX-RS Client - Ett rekomenderat sätt att koppla upp sig (framför URL)
      //https://howtodoinjava.com/jersey/jersey-restful-client-examples/
      //jersey-client dependencyn behöver även jersey-hk2 dependencyn av samma version för att funka.
      Client client = ClientBuilder.newClient();
      //Går att göra om Client och WebTarget så de går at återanvända
      //istället för att skapa dem i varje metod - Görs så nu för exemplets skull.
      WebTarget target = client.target(SakilaClient.BASE_URI);
      
      //Skapa ett Form-objekt som kan hålla formparametrarna från  ett application/x-www-form-urlencoded formulär
      Form form = new Form();
      form.param("first", txtFName.getText());
      form.param("last", txtLName.getText());
      
      //Skapa en anropsbyggare genom att använda target som håller URI...
      //... börja bygga en request och ange samtidigt vilken mediatyp som accepteras som respons.
      Invocation.Builder invocationBuilder = target.request(MediaType.APPLICATION_JSON);
      //Kör anropet med angivn metod, i detta fallet POST, 
      //och skickar med objektet i bodyn i form av en entitet av den angivna Mediatypen. 
      //Kan även vara .put(), .get() eller .delete()
      Response r = invocationBuilder.post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
      if (r.getStatus() != 201) {
         JOptionPane.showMessageDialog(this, "FEL: " + r.getStatus() + " Ange för- och efternamn!");
      } else {
         getActorsByURL();//Updatera tabellen för att även visa senaste
         //Visa sökvägen till den nyligt skpade posten
         JOptionPane.showMessageDialog(this, "Ny post lades till: " + r.getLocation());
      }
   }

   /**
    * This method is called from within the constructor to initialize the form.
    * WARNING: Do NOT modify this code. The content of this method is always
    * regenerated by the Form Editor.
    */
   @SuppressWarnings("unchecked")
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      jLabel1 = new javax.swing.JLabel();
      txtFName = new javax.swing.JTextField();
      jLabel2 = new javax.swing.JLabel();
      txtLName = new javax.swing.JTextField();
      btnAdd = new javax.swing.JButton();
      btnGetAll = new javax.swing.JButton();
      jLabel3 = new javax.swing.JLabel();
      txtId = new javax.swing.JTextField();
      btnDelete = new javax.swing.JButton();
      btnGet = new javax.swing.JButton();
      jScrollPane1 = new javax.swing.JScrollPane();
      tblActors = new javax.swing.JTable();

      setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

      jLabel1.setText("Förnamn:");

      jLabel2.setText("Efternamn:");

      btnAdd.setText("Lägg till");
      btnAdd.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            btnAddActionPerformed(evt);
         }
      });

      btnGetAll.setText("Hämta alla skådespelare");
      btnGetAll.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            btnGetAllActionPerformed(evt);
         }
      });

      jLabel3.setText("ID-nummer:");

      btnDelete.setText("Ta bort");

      btnGet.setText("Hämta namn");

      tblActors.setModel(new javax.swing.table.DefaultTableModel(
         new Object [][] {
            {null, null, null, null},
            {null, null, null, null},
            {null, null, null, null},
            {null, null, null, null}
         },
         new String [] {
            "Title 1", "Title 2", "Title 3", "Title 4"
         }
      ));
      jScrollPane1.setViewportView(tblActors);

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(jLabel1)
                     .addComponent(txtFName, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addGroup(layout.createSequentialGroup()
                        .addGap(13, 13, 13)
                        .addComponent(jLabel2))
                     .addGroup(layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                           .addComponent(btnGet)
                           .addComponent(txtLName, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                           .addComponent(btnDelete, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 77, javax.swing.GroupLayout.PREFERRED_SIZE)
                           .addComponent(btnAdd, javax.swing.GroupLayout.Alignment.TRAILING)))))
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                  .addGap(0, 0, Short.MAX_VALUE)
                  .addComponent(btnGetAll))
               .addGroup(layout.createSequentialGroup()
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(txtId)
                        .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                     .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 334, javax.swing.GroupLayout.PREFERRED_SIZE))
                  .addGap(0, 0, Short.MAX_VALUE)))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(jLabel1)
               .addComponent(jLabel2))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(txtFName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(txtLName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(btnAdd))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jLabel3)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addGroup(layout.createSequentialGroup()
                  .addGap(10, 10, 10)
                  .addComponent(txtId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
               .addGroup(layout.createSequentialGroup()
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                     .addComponent(btnGet)
                     .addComponent(btnDelete))))
            .addGap(24, 24, 24)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 275, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(btnGetAll)
            .addContainerGap(17, Short.MAX_VALUE))
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

   private void btnGetAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGetAllActionPerformed
      // TODO add your handling code here:
      try {
         getActorsByURL();
      } catch (Exception e) {
         JOptionPane.showMessageDialog(this, e.getMessage());
      }
   }//GEN-LAST:event_btnGetAllActionPerformed

   private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddActionPerformed
      // TODO add your handling code here:
//      addNewByGet();
      addNewByPost();
//      addNewByForm();
   }//GEN-LAST:event_btnAddActionPerformed

   /**
    * @param args the command line arguments
    */
   public static void main(String args[]) {
      /* Set the Nimbus look and feel */
      //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
      /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
       */
      try {
         for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) {
               javax.swing.UIManager.setLookAndFeel(info.getClassName());
               break;
            }
         }
      } catch (ClassNotFoundException ex) {
         java.util.logging.Logger.getLogger(SakilaClient.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
      } catch (InstantiationException ex) {
         java.util.logging.Logger.getLogger(SakilaClient.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
      } catch (IllegalAccessException ex) {
         java.util.logging.Logger.getLogger(SakilaClient.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
      } catch (javax.swing.UnsupportedLookAndFeelException ex) {
         java.util.logging.Logger.getLogger(SakilaClient.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
      }
      //</editor-fold>

      /* Create and display the form */
      java.awt.EventQueue.invokeLater(new Runnable() {
         public void run() {
            new SakilaClient().setVisible(true);
         }
      });
   }

   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JButton btnAdd;
   private javax.swing.JButton btnDelete;
   private javax.swing.JButton btnGet;
   private javax.swing.JButton btnGetAll;
   private javax.swing.JLabel jLabel1;
   private javax.swing.JLabel jLabel2;
   private javax.swing.JLabel jLabel3;
   private javax.swing.JScrollPane jScrollPane1;
   private javax.swing.JTable tblActors;
   private javax.swing.JTextField txtFName;
   private javax.swing.JTextField txtId;
   private javax.swing.JTextField txtLName;
   // End of variables declaration//GEN-END:variables
}
