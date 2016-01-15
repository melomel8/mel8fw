package dal;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import sql.DBResponse;
import sql.SQLParameter;
import sql.SQLQuerable;
import entities.BaseEntity;
import entities.BaseEntityList;
import entities.EntityAttribute;
import enumerations.ParameterDirection;
import filters.BaseFilter;

/**
 * Classe base per i metodi più comuni di accesso ad una base di dati attraverso driver JDBC
 * @author amelani
 *
 */
public abstract class BaseJDBCManager<TEntity extends BaseEntity, TEntityList extends BaseEntityList<TEntity>> 
{
	private static Context _initialContext; 
	private static DataSource _dataSource;
	
	/**
	 * Indica se si usa o meno un connection pool
	 * @return True se si usa un Connection Pool, false altrimenti
	 */
	public abstract boolean UsingConnectionPool();
	
	/**
	 * Indica il nome del Connection Pool, se usato.
	 * @return Nome del connection
	 */
	public abstract String GetConnectionPoolName();
	
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
	 * Nome della stored procedure di recupero di un solo record
	 * @return String che contiene il nome della stored procedure per il recupero di un solo record
	 */
	public abstract String GetProcedureName();
	
	/**
	 * Nome della stored procedure di lista
	 * @return String che contiene il nome della stored procedure per il recupero di una lista
	 */
	public abstract String GetListProcedureName();
	
	/**
	 * Costruttore base del JDBCManager, che registra il driver JDBC
	 * @throws SQLException In caso di errori nella registrazione del driver JDBC
	 * @throws NamingException In caso di errori nel recupero del contesto e del datasource (usando il connection pool)
	 */
	public BaseJDBCManager() throws SQLException, NamingException
	{
		if (UsingConnectionPool())
		{
			_initialContext = new InitialContext();
			_dataSource = (DataSource)_initialContext.lookup(GetConnectionString());
		}
		else
			DriverManager.registerDriver(GetJDBCDriver());
	}
	
	/**
	 * Restituisce una connessione al database. La connessione è già aperta senza autocommit.
	 * @return Connessione al Database
	 * @throws SQLException In caso di errori nella creazione della connessione
	 */
	private Connection _getConnection() throws SQLException
	{
		Connection theConnection;
		if (UsingConnectionPool())
			theConnection = _dataSource.getConnection();
		else
			theConnection = DriverManager.getConnection(GetConnectionString(), GetUsername(), GetPassword());
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
	private void _addParameters(SQLQuerable e, CallableStatement stmt) throws IllegalArgumentException, SQLException, IllegalAccessException
	{
		ArrayList<SQLParameter> params = e.GetParameters();
		if (params != null)
			for(SQLParameter param : params)
			{
				if (param.Direction == ParameterDirection.IN || param.Direction == ParameterDirection.INOUT)
					stmt.setObject(param.Name, param.Value);
				if (param.Direction == ParameterDirection.OUT || param.Direction == ParameterDirection.INOUT)
					stmt.registerOutParameter(param.Name, param.SqlType);
			}
	}
	
	/** Prepara un CallableStatement completo di parametri
	 * @param dbConnection Connessione al db da cui istanziare la Connection
	 * @param e SQLQuerable da cui prendere i parametri da agganciare al Callable statement
	 * @param procedureName Nome della procedura da eseguire
	 * @return CallableStatement completo di parametri
	 * @throws SQLException In caso di errori nella scrittura dei parametri nella procedura
	 * @throws IllegalAccessException Se il campo della classe non fosse accessibile
	 */
	private CallableStatement _prepareStatement(Connection dbConnection, SQLQuerable e, String procedureName) throws SQLException, IllegalAccessException 
	{
		EntityAttribute entityInfo = e.getClass().getAnnotation(EntityAttribute.class);
		CallableStatement callableStatement = dbConnection.prepareCall(procedureName);
		if (e.getClass() == BaseFilter.class || entityInfo != null)
			_addParameters(e, callableStatement);
		return callableStatement;
	}
	
	/**
	 * Resituisce una entity a partire da un ResultSet
	 * @param rs ResultSet che contiene i dati della entity
	 * @throws InstantiationException In caso di errori nella creazione dell'oggetto
	 * @return TEntity popolata
	 */
	public abstract TEntity EntityFromResultSet(ResultSet rs) throws InstantiationException;

	/**
	 * Salva una TEntity sulla base di dati
	 * @param e TEntity da salvare 
	 * @param listType Tipo della lista relativa alla entity da salvare
	 * @return DBResponse con il risultato dell'esecuzione della query
	 * @throws InstantiationException Nel caso di errori nella creazione dell'istanza della classe
	 * @throws IllegalAccessException Nel caso di errori nell'accesso ai campi della classe
	 * @throws SQLException In caso di errori nell'esecuzione delle procedure SQL
	 * @throws IllegalArgumentException In caso di anomalie nell'accesso ai campi della classe
	 */
	public DBResponse<TEntity, TEntityList> Save(TEntity e, Class<TEntityList> listType) throws InstantiationException, IllegalAccessException, IllegalArgumentException, SQLException
	{
		TEntityList theList = (TEntityList)listType.newInstance();
		theList.add(e);
		return Save(theList);
	}

	/**
	 * Salva una lista di oggetti TEntity sulla base di dati
	 * @param list Lista da salvare
	 * @return DBResponse con il risultato dell'esecuzione della query
	 * @throws SQLException In caso di errori nell'esecuzione delle procedure SQL
	 * @throws IllegalArgumentException In caso di anomalie nell'accesso ai campi della classe
	 * @throws IllegalAccessException Se almeno un campo della classe non è accessibile
	 */
	public DBResponse<TEntity, TEntityList> Save(TEntityList list) throws SQLException, IllegalArgumentException, IllegalAccessException
	{
		DBResponse<TEntity, TEntityList> theResponse = new DBResponse<TEntity, TEntityList>();
		Connection dbConnection = _getConnection();
		try
		{
			for (TEntity e : list)
			{
				CallableStatement callableStatement = _prepareStatement(dbConnection, e, GetSaveProcedureName());
				callableStatement.executeUpdate();
			}
			dbConnection.commit();
			theResponse.Success = true;
		}
		catch (SQLException exc)
		{
			theResponse.Success = false;
			theResponse.Message = exc.getLocalizedMessage();
			exc.printStackTrace();
			dbConnection.rollback();
		}
		finally
		{
			dbConnection.close();
		}
		
		return theResponse;
	}

	/**
	 * Cancella una TEntity dalla base di dati
	 * @param e TEntity da cancellare 
	 * @param listType Tipo della lista relativo alla entity da cancellare
	 * @return DBResponse con il risultato dell'esecuzione della query
	 * @throws InstantiationException Nel caso di errori nella creazione dell'istanza della classe
	 * @throws IllegalAccessException Nel caso di errori nell'accesso ai campi della classe
	 * @throws SQLException In caso di errori nell'esecuzione delle procedure SQL
	 * @throws IllegalArgumentException In caso di anomalie nell'accesso ai campi della classe
	 */
	public DBResponse<TEntity, TEntityList> Delete(TEntity e, Class<TEntityList> listType) throws InstantiationException, IllegalAccessException, IllegalArgumentException, SQLException
	{
		TEntityList theList = (TEntityList)listType.newInstance();
		theList.add(e);
		return Delete(theList);
	}

	/**
	 * Cancella una TEntityList dalla base di dati
	 * @param list TEntityList da cancellare 
	 * @return DBResponse con il risultato dell'esecuzione della query
	 * @throws InstantiationException Nel caso di errori nella creazione dell'istanza della classe
	 * @throws SQLException In caso di errori nell'esecuzione delle procedure SQL
	 */
	public DBResponse<TEntity, TEntityList> Delete(TEntityList list) throws IllegalAccessException, SQLException
	{
		DBResponse<TEntity, TEntityList> theResponse = new DBResponse<TEntity, TEntityList>();
		Connection dbConnection = _getConnection();
		try
		{
			for (TEntity e : list)
			{
				//Controllo che la classe sia annotata, se lo è procedo alla cancellazione
				CallableStatement callableStatement = _prepareStatement(dbConnection, e, GetDeleteProcedureName());
				callableStatement.executeUpdate();
			}
			dbConnection.commit();
			theResponse.Success = true;
		}
		catch (SQLException exc)
		{
			theResponse.Success = false;
			theResponse.Message = exc.getLocalizedMessage();
			exc.printStackTrace();
			dbConnection.rollback();
		}
		finally
		{
			dbConnection.close();
		}
		return theResponse;
	}
		
	/**
	 * Esegue la procedura di cancellazione dalla base di dati con dei filtri
	 * @param filter Filtri da eseguire in cancellazione
	 * @return DBResponse con il risultato dell'esecuzione della query
	 * @throws SQLException
	 * @throws IllegalAccessException
	 */
	public DBResponse<TEntity, TEntityList> Delete(BaseFilter<TEntity> filter) throws SQLException, IllegalAccessException
	{
		DBResponse<TEntity, TEntityList> theResponse = new DBResponse<TEntity, TEntityList>();
		Connection dbConnection = _getConnection();
		CallableStatement callableStatement = _prepareStatement(dbConnection, filter, GetDeleteProcedureName());
		try
		{
			callableStatement.executeUpdate();
			dbConnection.commit();
			theResponse.Success = true;
		}
		catch (SQLException exc)
		{
			theResponse.Success = false;
			theResponse.Message = exc.getLocalizedMessage();
			exc.printStackTrace();
			dbConnection.rollback();
		}
		finally
		{
			dbConnection.close();
		}
		return theResponse;
		
	}
	
	/**
	 * Recupera una entity basandosi sulle proprietà racchiuse in un filtro
	 * @param filter Filtro per recuperare la entity
	 * @return DBResponse con il risultato dell'esecuzione della query
	 * @throws SQLException In caso di errori nell'esecuzione delle procedure SQL
	 * @throws IllegalAccessException Nel caso di errori nell'accesso ai campi della classe
	 */
	public DBResponse<TEntity, TEntityList> Get(BaseFilter<TEntity> filter) throws SQLException, IllegalAccessException 
	{
		Connection dbConnection = _getConnection();
		DBResponse<TEntity, TEntityList> theResponse = new DBResponse<TEntity, TEntityList>();
		dbConnection.setAutoCommit(true);
		CallableStatement callableStatement = _prepareStatement(dbConnection, filter, GetProcedureName());
		ResultSet rs = callableStatement.executeQuery();
		try
		{
			TEntity resultEntity = EntityFromResultSet(rs);
			theResponse.Success = true;
			theResponse.Data.add(resultEntity);
		}
		catch (InstantiationException exc)
		{
			theResponse.Success = false;
			theResponse.Message = exc.getLocalizedMessage();
			exc.printStackTrace();
		}
		finally
		{
			dbConnection.close();
		}
		return theResponse;
	}
	
	/**
	 * Recupera una lista di entitò basandosi sulle proprietà racchiuse in un filtro
	 * @param filter Filtro per recuperare la entity list
	 * @param listType Tipo di lista da costruire
	 * @return DBResponse con il risultato dell'esecuzione della query
	 * @throws SQLException In caso di errori nell'esecuzione delle procedure SQL
	 * @throws IllegalAccessException Nel caso di errori nell'accesso ai campi della classe
	 * @throws InstantiationException Se la classe che si sta tentando di costruire non può essere istanziata
	 */
	public DBResponse<TEntity, TEntityList> List(BaseFilter<TEntity> filter, Class<TEntityList> listType) throws SQLException, IllegalAccessException, InstantiationException 
	{
		return ExecuteSelection(filter, GetListProcedureName(), listType);
	}
	
	/**
	 * Esegue una query di selezione con determinati parametri
	 * @param filter Filtro di ricerca
	 * @param procedureName Nome della procedura
	 * @param listType Tipo di lista del ritorno
	 * @return DBResponse con il risultato dell'esecuzione della query
	 * @throws SQLException In caso di errori nell'esecuzione delle procedure SQL
	 * @throws IllegalAccessException Nel caso di errori nell'accesso ai campi della classe
	 * @throws InstantiationException Se la classe che si sta tentando di costruire non può essere istanziata
	 */
	public DBResponse<TEntity, TEntityList> ExecuteSelection(BaseFilter<TEntity> filter, String procedureName, Class<TEntityList> listType) throws SQLException, IllegalAccessException, InstantiationException
	{
		DBResponse<TEntity, TEntityList> theResponse = new DBResponse<TEntity, TEntityList>();
		Connection dbConnection = _getConnection();
		dbConnection.setAutoCommit(true);
		CallableStatement callableStatement = _prepareStatement(dbConnection, filter, procedureName);
		ResultSet rs = callableStatement.executeQuery();
		TEntityList resultList = (TEntityList)listType.newInstance();
		try
		{
			while (rs.next())
				resultList.add(EntityFromResultSet(rs));
			theResponse.Success = true;
			theResponse.Data = resultList;
		}
		catch (InstantiationException exc)
		{
			theResponse.Success = false;
			theResponse.Message = exc.getLocalizedMessage();
			exc.printStackTrace();
		}
		finally
		{
			dbConnection.close();
		}
		return theResponse;
	}
	
	/**
	 * Esegue una query di aggiornamento con determinati parametri
	 * @param filter Filtro contenente i parametri per l'aggiornamento
	 * @param procedureName Nome della procedura
	 * @return DBResponse con il risultato dell'esecuzione della query
	 * @throws SQLException In caso di errori nell'esecuzione delle procedure SQL
	 * @throws IllegalAccessException Nel caso di errori nell'accesso ai campi della classe
	 */
	public DBResponse<TEntity, TEntityList> ExecuteUpdate(BaseFilter<TEntity> filter, String procedureName) throws SQLException, IllegalAccessException
	{
		DBResponse<TEntity, TEntityList> theResponse = new DBResponse<TEntity, TEntityList>();
		Connection dbConnection = _getConnection();
		CallableStatement callableStatement = _prepareStatement(dbConnection, filter, procedureName);
		try
		{
			callableStatement.executeUpdate();
			dbConnection.commit();
			theResponse.Success = true;
		}
		catch (SQLException exc)
		{
			theResponse.Success = false;
			theResponse.Message = exc.getLocalizedMessage();
			exc.printStackTrace();
			dbConnection.rollback();
		}
		finally
		{
			dbConnection.close();
		}
		return theResponse;
	}
}