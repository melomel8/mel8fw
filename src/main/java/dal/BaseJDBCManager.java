package dal;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

import entities.BaseEntity;
import entities.BaseEntityList;
import entities.EntityAttribute;
import entities.FieldAttribute;
import enumerations.SQLDirection;

/**
 * Classe base per i metodi più comuni di accesso ad una base di dati attraverso driver JDBC
 * @author amelani
 *
 */
public abstract class BaseJDBCManager<TEntity extends BaseEntity, TEntityList extends BaseEntityList<TEntity>> 
{
	/**
	 * Connection string per la connessione al databasee
	 * @return Stringa che contiene la ConnectionString per la connessione al DB
	 */
	public abstract String GetConnectionString();

	/**
	 * Nome utente con cui collegarsi al DB
	 * @return String che contiene la username per collegarsi al DB
	 */
	public abstract String GetUsername();

	/**
	 * Password per collegarsi al DB
	 * @return String che contiene la password di collegamento al DB 
	 */
	public abstract String GetPassword();

	/**
	 * Driver JDBC per l'interfacciamento alla base di dati
	 * @return Driver JDBC per l'interfacciamento alla base di dati
	 */
	public abstract Driver GetJDBCDriver();
	
	/**
	 * Nome della stored procedure di salvataggio
	 * @return String che contiene il nome della stored procedure per il salvataggio
	 */
	public abstract String GetSaveProcedureName();
	
	/**
	 * Nome della stored procedure di cancellazione
	 * @return String che contiene il nome della stored procedure per la cancellazione
	 */
	public abstract String GetDeleteProcedureName();

	/**
	 * Costruttore base del JDBCManager, che registra il driver JDBC
	 * @throws SQLException In caso di errori nella registrazione del driver JDBC
	 */
	public BaseJDBCManager() throws SQLException
	{
		DriverManager.registerDriver(GetJDBCDriver());
	}

	/**
	 * Restituisce una connessione al database. La connessione è già aperta.
	 * @return Connessione al Database
	 * @throws SQLException In caso di errori nella creazione della connessione
	 */
	private Connection _getConnection() throws SQLException
	{
		String connectionString = GetConnectionString() + GetUsername() + GetPassword();
		Connection theConnection = DriverManager.getConnection(connectionString);
		theConnection.setAutoCommit(false);
		return theConnection;
	}
	
	/**
	 * Aggiunge una serie di parametri ad un CallableStatement recuperandoli dai campi di una classe
	 * @param e Classe da cui prendere i parametri
	 * @param stmt CallableStatement a cui aggiungere i parametri
	 * @throws IllegalArgumentException Se il campo che si tenta di accedere non appartiene alla classe
	 * @throws SQLException Se il nome del parametro in corrisponde a quello della procedura 
	 * @throws IllegalAccessException Se il campo della classe non è accessibile
	 */
	private void _addParameters(TEntity e, CallableStatement stmt) throws IllegalArgumentException, SQLException, IllegalAccessException
	{
		//recupero tutti i campi della classe
		Field[] fields = e.getClass().getFields();
		for (Field f : fields)
		{
			/*
			 * Per ogni campo controllo:
			 * 1. Che sia pubblico
			 * 2. Che sia annotato con l'annotazione FieldAttribute
			 * Se rispettano queste condizioni, aggiungo il parametro nella maniera opportuna al CallableStatement
			 */
			int fModifiers = f.getModifiers();
			FieldAttribute fieldInfo = f.getAnnotation(FieldAttribute.class); 
			if (Modifier.isPublic(fModifiers) && fieldInfo != null)
			{
				//recupero il nome del parametro e lo aggiungo
				String parameterName = fieldInfo.ParameterName().trim().equals("") ? f.getName() : fieldInfo.ParameterName().trim();
				if (fieldInfo.Direction() == SQLDirection.IN || fieldInfo.Direction() == SQLDirection.INOUT)
					stmt.setObject(parameterName, f.get(this));
				if (fieldInfo.Direction() == SQLDirection.OUT || fieldInfo.Direction() == SQLDirection.INOUT)
					stmt.registerOutParameter(parameterName, fieldInfo.SqlType());
			}
		}
	}

	/**
	 * Salva una TEntity sulla base di dati
	 * @param e TEntity da salvare 
	 * @return True in caso di successo, false altrimenti
	 * @throws InstantiationException Nel caso di errori nella creazione dell'istanza della classe
	 * @throws IllegalAccessException Nel caso di errori nell'accesso ai campi della classe
	 * @throws SQLException In caso di errori nell'esecuzione delle procedure SQL
	 * @throws IllegalArgumentException In caso di anomalie nell'accesso ai campi della classe
	 */
	@SuppressWarnings({ "unchecked", "null" })
	public boolean Save(TEntity e) throws InstantiationException, IllegalAccessException, IllegalArgumentException, SQLException
	{
		TEntityList theList = null;
		theList = (TEntityList)theList.getClass().newInstance();
		theList.add(e);
		return Save(theList);
	}

	/**
	 * Salva una lista di oggetti TEntity sulla base di dati
	 * @param list Lista da salvare
	 * @return True in caso di successo, false altrimenti
	 * @throws SQLException In caso di errori nell'esecuzione delle procedure SQL
	 * @throws IllegalArgumentException In caso di anomalie nell'accesso ai campi della classe
	 * @throws IllegalAccessException Se almeno un campo della classe non è accessibile
	 */
	public boolean Save(TEntityList list) throws SQLException, IllegalArgumentException, IllegalAccessException
	{
		boolean saveResult = false;
		Connection dbConnection = _getConnection();
		for (TEntity e : list)
		{
			//Controllo che la classe sia annotata, se lo è procedo al salvataggio
			EntityAttribute entityInfo = e.getClass().getAnnotation(EntityAttribute.class);
			CallableStatement callableStatement = dbConnection.prepareCall(GetSaveProcedureName());
			if (entityInfo != null)
				_addParameters(e, callableStatement);
			callableStatement.executeUpdate();
		}
		try
		{
			dbConnection.commit();
			saveResult = true;
		}
		catch (SQLException exc)
		{
			saveResult = false;
			exc.printStackTrace();
			dbConnection.rollback();
		}
		finally
		{
			dbConnection.close();
		}
		return saveResult;
	}
	
	/**
	 * Cancella una TEntity dalla base di dati
	 * @param e TEntity da cancellare 
	 * @return True in caso di successo, false altrimenti
	 * @throws InstantiationException Nel caso di errori nella creazione dell'istanza della classe
	 * @throws IllegalAccessException Nel caso di errori nell'accesso ai campi della classe
	 * @throws SQLException In caso di errori nell'esecuzione delle procedure SQL
	 * @throws IllegalArgumentException In caso di anomalie nell'accesso ai campi della classe
	 */
	@SuppressWarnings({ "unchecked", "null" })
	public boolean Delete(TEntity e) throws InstantiationException, IllegalAccessException, IllegalArgumentException, SQLException
	{
		TEntityList theList = null;
		theList = (TEntityList)theList.getClass().newInstance();
		theList.add(e);
		return Save(theList);
	}
	
	public boolean Delete(TEntityList list)
	{
		return false;
	}
}
