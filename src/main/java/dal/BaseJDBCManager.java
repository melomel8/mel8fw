package dal;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

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
	 * @return TEntity popolata
	 */
	public abstract TEntity EntityFromResultSet(ResultSet rs);

	/**
	 * Salva una TEntity sulla base di dati
	 * @param e TEntity da salvare 
	 * @param listType Tipo della lista relativa alla entity da salvare
	 * @return True in caso di successo, false altrimenti
	 * @throws InstantiationException Nel caso di errori nella creazione dell'istanza della classe
	 * @throws IllegalAccessException Nel caso di errori nell'accesso ai campi della classe
	 * @throws SQLException In caso di errori nell'esecuzione delle procedure SQL
	 * @throws IllegalArgumentException In caso di anomalie nell'accesso ai campi della classe
	 */
	public boolean Save(TEntity e, Class<TEntityList> listType) throws InstantiationException, IllegalAccessException, IllegalArgumentException, SQLException
	{
		TEntityList theList = (TEntityList)listType.newInstance();
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
			CallableStatement callableStatement = _prepareStatement(dbConnection, e, GetSaveProcedureName());
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
	 * @param listType Tipo della lista relativo alla entity da cancellare
	 * @return True in caso di successo, false altrimenti
	 * @throws InstantiationException Nel caso di errori nella creazione dell'istanza della classe
	 * @throws IllegalAccessException Nel caso di errori nell'accesso ai campi della classe
	 * @throws SQLException In caso di errori nell'esecuzione delle procedure SQL
	 * @throws IllegalArgumentException In caso di anomalie nell'accesso ai campi della classe
	 */
	public boolean Delete(TEntity e, Class<TEntityList> listType) throws InstantiationException, IllegalAccessException, IllegalArgumentException, SQLException
	{
		TEntityList theList = (TEntityList)listType.newInstance();
		theList.add(e);
		return Save(theList);
	}

	/**
	 * Cancella una TEntityList dalla base di dati
	 * @param list TEntityList da cancellare 
	 * @return True in caso di successo, false altrimenti
	 * @throws InstantiationException Nel caso di errori nella creazione dell'istanza della classe
	 * @throws SQLException In caso di errori nell'esecuzione delle procedure SQL
	 */
	public boolean Delete(TEntityList list) throws IllegalAccessException, SQLException
	{
		boolean deleteResult = false;
		Connection dbConnection = _getConnection();
		for (TEntity e : list)
		{
			//Controllo che la classe sia annotata, se lo è procedo alla cancellazione
			CallableStatement callableStatement = _prepareStatement(dbConnection, e, GetDeleteProcedureName());
			callableStatement.executeUpdate();
		}
		try
		{
			dbConnection.commit();
			deleteResult = true;
		}
		catch (SQLException exc)
		{
			deleteResult = false;
			exc.printStackTrace();
			dbConnection.rollback();
		}
		finally
		{
			dbConnection.close();
		}
		return deleteResult;
	}
		
	/**
	 * Recupera una entity basandosi sulle proprietà racchiuse in un filtro
	 * @param filter Filtro per recuperare la entity
	 * @param entityType Tipo dell'entità da costruire
	 * @return TEntity popolata dall'esecuzioned della query
	 * @throws SQLException In caso di errori nell'esecuzione delle procedure SQL
	 * @throws IllegalAccessException Nel caso di errori nell'accesso ai campi della classe
	 */
	public TEntity Get(BaseFilter<TEntity> filter, Class<TEntity> entityType) throws SQLException, IllegalAccessException 
	{
		Connection dbConnection = _getConnection();
		dbConnection.setAutoCommit(true);
		CallableStatement callableStatement = _prepareStatement(dbConnection, filter, GetProcedureName());
		ResultSet rs = callableStatement.executeQuery();
		TEntity resultEntity = EntityFromResultSet(rs);
		dbConnection.close();
		return resultEntity;
	}
	
	/**
	 * Recupera una lista di entitò basandosi sulle proprietà racchiuse in un filtro
	 * @param filter Filtro per recuperare la entity list
	 * @param listType Tipo di lista da costruire
	 * @return TEntityList popolata dall'esecuzioned della query
	 * @throws SQLException In caso di errori nell'esecuzione delle procedure SQL
	 * @throws IllegalAccessException Nel caso di errori nell'accesso ai campi della classe
	 * @throws InstantiationException Se la classe che si sta tentando di costruire non può essere istanziata
	 */
	public TEntityList List(BaseFilter<TEntity> filter, Class<TEntityList> listType) throws SQLException, IllegalAccessException, InstantiationException 
	{
		Connection dbConnection = _getConnection();
		dbConnection.setAutoCommit(true);
		CallableStatement callableStatement = _prepareStatement(dbConnection, filter, GetListProcedureName());
		ResultSet rs = callableStatement.executeQuery();
		TEntityList resultList = (TEntityList)listType.newInstance();
		while (rs.next())
			resultList.add(EntityFromResultSet(rs));		
		dbConnection.close();
		return resultList.size() == 0 ? null : resultList;
	}
}