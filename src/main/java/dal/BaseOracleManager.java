package dal;

import java.sql.Driver;
import java.sql.SQLException;

import javax.naming.NamingException;

import entities.BaseEntity;
import entities.BaseEntityList;

public abstract class BaseOracleManager <TEntity extends BaseEntity, TEntityList extends BaseEntityList<TEntity>> extends BaseJDBCManager<TEntity, TEntityList>
{
	@Override
	public Driver GetJDBCDriver() 
	{
		return new oracle.jdbc.OracleDriver();
	}
	
	/**
	 * Restituisce il SID del database Oracle a cui connettersi
	 * @return Stringa contenente il SID del database
	 */
	public abstract String GetSID();
	
	/**
	 * Restituisce il nome del server nel formato hostname:port
	 * @return Stringa che contiene il nome del server
	 */
	public abstract String GetServerName();
	
	@Override
	public String GetConnectionString() 
	{
		return "jdbc:oracle:thin:@" + GetServerName() + ":" + GetSID();
	}
	
	public BaseOracleManager() throws SQLException, NamingException
	{
		super();
	}
	
	
}
