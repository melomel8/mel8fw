package entities;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import sql.SQLParameter;
import sql.SQLQuerable;

/**
 * Classe base per la derivazione delle entità
 * @author amelani
 *
 */
public abstract class BaseEntity implements SQLQuerable
{
	public BaseEntity() 
	{
		
	}
	
	public BaseEntity(ResultSet rs) throws SQLException, IllegalArgumentException, IllegalAccessException, UnsupportedEncodingException
	{
		this();
		Field[] classFields = this.getClass().getFields();
		for (int i = 0; i < classFields.length; i++)
		{
			Field currentField = classFields[i];
			EntityFieldAttribute attributes = currentField.getAnnotation(EntityFieldAttribute.class);
			if (attributes != null)
			{			
				try
				{
					Object sqlValue = rs.getObject(attributes.Name());
					currentField.set(this, sqlValue);	
				}
				catch (SQLException e)
				{
					currentField.set(this, null);
				}
			}
		}
	}
	
	/**
	 * Restituisce un ArrayList di parametri SQL recuperati dai valori delle proprietà della classe
	 * @return ArrayList<SQLParameters> con i parametri SQL
	 * @throws IllegalArgumentException Se la proprietà che si sta tentando di accedere non appartiene alla classe
	 * @throws IllegalAccessException Se la proprietà della classe non è accessibile
	 */
	@Override
	public ArrayList<SQLParameter> GetParameters() throws IllegalArgumentException, IllegalAccessException
	{
		ArrayList<SQLParameter> params = new ArrayList<SQLParameter>();
		
		//recupero tutti i campi della classe (anche se considererò soltanto quelli pubblici)
		Field[] fields = this.getClass().getFields();
		for (Field currentField : fields)
		{
			EntityFieldAttribute attributes = currentField.getAnnotation(EntityFieldAttribute.class);
			if (Modifier.isPublic(currentField.getModifiers()) && attributes != null)
			{
				//il campo è pubblico ed è annotato, lo posso considerare come parametro SQL
				String name = attributes.Name().trim().equals("") ? currentField.getName() : attributes.Name().trim();
				int sqlType = attributes.SqlType();
				SQLParameter theParam = null;
				if (sqlType != -1)
					theParam = new SQLParameter(name, currentField.get(this), sqlType, attributes.Direction());
				else
					theParam = new SQLParameter(name, currentField.get(this), attributes.Direction());
				params.add(theParam);
			}
		}
		
		return params.size() == 0 ? null : params;
	}

	@Override
	public String toString()
	{
		//recupero tutti i campi tramite Reflection
		StringBuilder result = new StringBuilder();
		try
		{
			for (Field currentClassField : this.getClass().getFields())
				result.append(currentClassField.getName() + ":" + currentClassField.get(this).toString());
		}
		catch (Exception e)
		{
			e.printStackTrace();

		}
		return result.toString();
	}
}