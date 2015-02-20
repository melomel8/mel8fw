package entities;

import java.lang.reflect.Field;
import java.sql.ResultSet;

import org.apache.commons.lang.NotImplementedException;

public class BaseEntity 
{
	public BaseEntity()
	{

	}

	public BaseEntity(ResultSet rs) throws NotImplementedException
	{
		throw new NotImplementedException("Method not yet implemented");
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
