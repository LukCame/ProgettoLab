package it.surveys.model;

import it.surveys.util.UtilDB;
import java.sql.*;

/**
 * La funzionalita' della classe DCS (Domain Control Service) e' simile a quella della classe
 * DAO; a differenza di essa realizza delle feature addizionali che non riguardano un
 * particolare oggetto del dominio.
 * La classe DCS e' accessibile esclusivamente tramite i manager.
 * @author L.Camerlengo
 * @version 1.1, 26/02/2016
 */
public class SurveyDCS {
    
    /**
     * Effettua una formattazione in una tabella dei risultati del sondaggio passato in ingresso;
     * la tabella contiene per ogni riga una determinata risposta tra le possibili e una percentuale che esprime 
     * la porzione di utenti che l'hanno scelta;
     * se una risposta non e' mai stata selezionata da un utente la sua percentuale e' 0%.
     * Restituisce una stringa formattata in HTML contenente una tabella con i risultati di un determinato sondaggio.
     * @param idSurvey int
     * @return String risultati di un determinato sondaggio
     */
    public static String displayResults(int idSurvey){
        UtilDB utl=UtilDB.getUtilDB();
        Connection conn=null;
        Statement stm=null;
        Statement stm2=null;
        String results=null;
        try{
            conn=utl.createConnection();
            stm=utl.createStatement(conn);
            stm2=utl.createStatement(conn);
            String query1="select answer1,answer2,answer3,answer4 from survey "+ 
                          "where id="+idSurvey;
            String query2="select answer,count(answer) numAnswer "+
                           "from answer where idSurvey="+idSurvey+" group by answer";
            ResultSet result1=utl.query(stm, query1);
            ResultSet result2=utl.query(stm2, query2);
            int numAnswer=0;
            results="<table><tr>"+ 
                        "<th>Risposta</th>"+
                        "<th>Risultato</th>"+
                    "</tr>";
            while(result2.next()){
                numAnswer=numAnswer+result2.getInt(2);
            }
            result2.beforeFirst();
            int percentage;
            while(result2.next()){
                results=results+"<tr>";
                percentage=Math.round((result2.getInt(2)*100)/numAnswer);
                results=results+"<td>"+result2.getString(1)+"</td>"+"<td>"+percentage+"%</td>";
                results=results+"</tr>";
            }
            result1.next();
            for(int i=1; i<=4; i++) {	//ricerca di risposte mai selezionate dagli utenti
            	String answer = result1.getString(i);
            	if(answer == null)
            		break;
            	if(!results.contains(answer)) {
                    results=results+"<tr>";
                    results=results+"<td>"+answer+"</td>"+"<td>0%</td>";
                    results=results+"</tr>";
            	}
            }
            results=results+"</table>";
       } catch(ClassNotFoundException e){
            System.err.println("Driver Not Found!");
	    e.printStackTrace();
            return "fail";
       } catch(SQLException e){
            System.err.println("Database Error!");
	    e.printStackTrace();
            return "fail";
       } finally{
            try{
            if(stm!=null)
                utl.closeStatement(stm);
            if(conn!=null)
                utl.closeConnection(conn);
            } catch(SQLException e){
                System.err.println("Close Resource Error!");
                e.printStackTrace();
                return "fail";
            }
        }
        return results;
    }
    
    /**
     * Effettua una formattazione in una tabella dei sondaggi presenti nel database;
     * la tabella contiene per ogni riga la domanda del sondaggio, un pulsante "Visualizza" che permette al
     * responsabile di visionare l'andamento delle risposte e un pulsante cancella che consente di cancellare
     * il sondaggio dal database.
     * Restituisce una stringa formattata in HTML contenente una tabella con i sondaggi presenti nel database, altrimenti
     * se non e' presente alcun sondaggio nel database viene restiuito un messaggio sotto forma di stringa.
     * @return String sondaggi presenti nel database.
     */
    public static String displayCreatedSurveys(){
        UtilDB utl=UtilDB.getUtilDB();
        Connection conn=null;
        Statement stm=null;
        String surveys=null;
        try{
            conn=utl.createConnection();
            stm=utl.createStatement(conn);
            String query="select question,id from survey";
            ResultSet result=utl.query(stm, query);
             if(!result.next()){
                return "<p>Non sono presenti sondaggi.</p>";
            }
            surveys="<table><tr>"+
                        "<th>Sondaggio</th>"+
                        "<th>Risultati</th>"+
                        "<th>Cancellazione</th>"+
                    "</tr>";                   
           
            result.beforeFirst();
            while(result.next()){
                surveys=surveys+"<tr>";
                surveys=surveys+"<td>"+result.getString(1)+"</td>"+
                        "<td><a href=\"displayResults.action?id="+result.getString(2)+"\">Visualizza</a></td>"+
                        "<td><a onclick=\"confirmation("+result.getString(2)+")\">Cancella</a></td>";                      
                surveys=surveys+"</tr>";
            }
            surveys=surveys+"</table>";
       } catch(ClassNotFoundException e){
            System.err.println("Driver Not Found!");
            e.printStackTrace();
            return "fail";
       } catch(SQLException e){
            System.err.println("Database Error!");
            e.printStackTrace();
            return "fail";
       } finally{
            try{
	            if(stm!=null)
	                utl.closeStatement(stm);
	            if(conn!=null)
	                utl.closeConnection(conn);
            } catch(SQLException e){
                System.err.println("Close Resource Error!");
                e.printStackTrace();
                return "fail";
            }
        }
        return surveys;
    }
    
    /**
     * Effettua una formattazione in una tabella dei sondaggi appartenenti alle categorie di interesse 
     * per l'utente passato in ingresso a cui ancora non ha risposto;
     * la tabella contiene per ogni riga la domanda del sondaggio e un pulsante visualizza che permette all'utente di
     * visualizzare per intero il sondaggio e di poter rispondere.
     * Restituisce una stringa formattata in HTML contenente una tabella con i sondaggi appartenenti alle 
     * categoire di interesse per l'utente presenti nel database, altrimenti se non e' presente
     * alcun sondaggio associato alle categorie di interesse dell'utente restituisce un
     * messaggio sotto forma di stringa.
     * @param idUser int
     * @return String sondaggi appartenenti alle categorie preferite dell'utente. 
     */
    public static String displayAllowedSurvey(int idUser){
        UtilDB utl=UtilDB.getUtilDB();
        Connection conn=null;
        Statement stm=null;
        String surveys=null;
        try{
            conn=utl.createConnection();
            stm=utl.createStatement(conn);
            String query="SELECT DISTINCT s.question, s.id FROM categoriesUser c, categoriesSurvey c1, survey s "+
                         "WHERE c.idUser="+idUser+" AND c.idCategory=c1.idCategory AND c1.idSurvey=s.id AND (s.id,c.idUser) "+
                         "NOT IN (select a.idSurvey,a.idUser from answer a)";
            ResultSet res=utl.query(stm, query);
            if(!res.next()){
                return "<p>Attenzione: o non sono presenti sondaggi per le categorie scelte oppure sono state "+
                		"cancellate tutte le categorie scelte, in questo caso vai su 'Modifica profilo' per sceglierne altre.</p>";
            }
            surveys="<table><tr>"+
                        "<th>Sondaggio</th>"+
                        "<th>Risposta</th>"+
                    "</tr>";
            res.beforeFirst();
            while(res.next()){
                surveys=surveys+"<tr>";
                surveys=surveys+"<td>"+res.getString(1)+"</td>"+
                        "<td><a href=\"displaySurvey.action?id="+res.getString(2)+"\">Rispondi</a></td>";
                surveys=surveys+"</tr>";
            }
            surveys=surveys+"</table>";                   
       }catch(ClassNotFoundException e){
            System.err.println("Driver Not Found!");
            e.printStackTrace();
            return "fail";
       }catch(SQLException e){
            System.err.println("Database Error!");
            e.printStackTrace();
            return "fail";
       } finally{
            try{
	            if(stm!=null)
	                utl.closeStatement(stm);
	            if(conn!=null)
	                utl.closeConnection(conn);
            } catch(SQLException e){
                System.err.println("Close Resource Error!");
                e.printStackTrace();
                return "fail";
            }
        }
        return surveys;
    }
    
    /**
     * Effettua l'operazione di inserimento nel database di una risposta ad un determinato sondaggio 
     * da parte di un determinato utente.
     * Restituisce "success" se l'inserimento e' andato a buon fine, "fail" altrimenti.
     * @param idSurvey int
     * @param idUser int
     * @param answer String
     * @return String esito dell'inserimento della risposta
     */
    public static String insertAnswer(int idSurvey, int idUser, String answer){
        UtilDB utl=UtilDB.getUtilDB();
        Connection conn=null;
        Statement stm=null;
        try{
            conn=utl.createConnection();
            stm=utl.createStatement(conn);
            String insert="insert into answer values("+idUser+","+idSurvey+",'"+answer+"')";
            utl.manipulate(stm, insert);
       } catch(ClassNotFoundException e){
            System.err.println("Driver Not Found!");
            e.printStackTrace();
            return "fail";
       } catch(SQLException e){
            System.err.println("Database Error!");
            e.printStackTrace();
            return "fail";
       } finally{
            try{
            if(stm!=null)
                utl.closeStatement(stm);
            if(conn!=null)
                utl.closeConnection(conn);
            } catch(SQLException e){
                System.err.println("Close Resource Error!");
                e.printStackTrace();
                return "fail";
            }
        }  
        return "success";
    }
    
    /**
     * Effettua l'operazione di cancellazione nel database dei sondaggi che non hanno categorie associate.
     * Restituisce "success" se la cancellazione e' andata a buon fine o non esistono sondaggi da cancellare,
     * "fail" altrimenti.
     * @return String esito della cancellazione
     */
    public static String deleteSurveysWithoutCategories(){
        UtilDB utl=UtilDB.getUtilDB();
        Connection conn=null;
        Statement stm=null;
        Statement stm2=null;
        try{
            conn=utl.createConnection();
            stm=utl.createStatement(conn);
            stm2=utl.createStatement(conn);
            String query="select s.id from survey s where (s.id) not in "+
                         "(select cs.idSurvey from categoriesSurvey cs)";
            String delete;
            ResultSet result=utl.query(stm, query);
            while(result.next()){
                delete="delete from survey where id="+result.getString(1);
                int rows=utl.manipulate(stm2, delete);
                if(rows!=1){
                    System.err.println("Delete Database Error!");
                    return "fail";
                }
            }
       } catch(ClassNotFoundException e){
            System.err.println("Driver Not Found!");
            e.printStackTrace();
            return "fail";
       } catch(SQLException e){
            System.err.println("Database Error!");
            e.printStackTrace();
            return "fail";
       } finally{
            try{
	            if(stm!=null)
	                utl.closeStatement(stm);
	            if(stm2!=null)
	                utl.closeStatement(stm2);
	            if(conn!=null)
	                utl.closeConnection(conn);
            } catch(SQLException e){
                System.err.println("Close Resource Error!");
                e.printStackTrace();
            }
        }
        return "success";
    }
    
    /**
     * Effettua un inserimento nel database delle associazioni del sondaggio con le categorie passate in ingresso. 
     * Restituisce "success" se l'inserimento delle associazioni e' andato a buon fine, "fail" altrimenti.
     * @param idSurvey int
     * @param categories int[] array degli id delle categorie
     * @return String esito dell'inserimento
     */
    public static String insertCategoriesAssociation(int idSurvey,int[] categories){
        UtilDB utl=UtilDB.getUtilDB();
        Connection conn=null;
        Statement stm=null;
        try{
	        conn=utl.createConnection();
	        stm=utl.createStatement(conn);
	        String query;
	        for(int c:categories){
	            query="insert into categoriesSurvey values("+idSurvey+","+c+")";
	            int rows=utl.manipulate(stm, query);
	            if(rows!=1){
	                System.err.println("Insert Database Error!");
	                return "fail";
	            }
	        }
        } catch(ClassNotFoundException e){
            System.err.println("Driver Not Found!");
            e.printStackTrace();
            return "fail";
       } catch(SQLException e){
            System.err.println("Database Error!");
            e.printStackTrace();
            return "fail";
       } finally{
            try{
            if(stm!=null)
                utl.closeStatement(stm);
            if(conn!=null)
                utl.closeConnection(conn);
            } catch(SQLException e){
                System.err.println("Close Resource Error!");
                e.printStackTrace();
            }
        }
        return "success";
    }
}
