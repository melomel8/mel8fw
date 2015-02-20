package sql;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.math.BigDecimal;

import enumerations.ParameterDirection;

/**
 * Classe che modella un parametro SQL
 * @author amelani
 *
 */
public class SQLParameter 
{
	/** Nome del parametro */
	public String Name;
	
	/** Valore del parametro */
	public Object Value;
	
	/** Tipo del parametro */
	public int SqlType;
	
	/** Direzione del parametro */
	public ParameterDirection Direction;
	
	/**
	 * Restituisce il tipo SQL a seconda del tipo assunto dall'oggetto. I tipi SQL presi in considerazione sono: VARCHAR, DECIMAL, BIT, INTEGER, BIGINT, REAL, FLOAT, VARBINARY, DATE, TIME, TIMESTAMP.
	 * @param fieldType Tipo dell'oggetto
	 * @return Tipo SQL su cui si rimappa l'oggetto.
	 */
	private static int _getSQLTypeFromValue(Class<?> fieldType)
	{
		int sqlType = -1;
		if (fieldType == String.class)
			sqlType = Types.VARCHAR;
		else if (fieldType == BigDecimal.class )
			sqlType = Types.DECIMAL;
		else if (fieldType == Boolean.class || fieldType == boolean.class)
			sqlType = Types.BIT;
		else if (fieldType == Integer.class || fieldType == int.class)
			sqlType = Types.INTEGER;
		else if (fieldType == Long.class || fieldType == long.class)
			sqlType = Types.BIGINT;
		else if (fieldType == Float.class || fieldType == float.class)
			sqlType = Types.REAL;
		else if (fieldType == Double.class || fieldType == double.class)
			sqlType = Types.FLOAT;
		else if (fieldType == byte.class)
			sqlType = Types.VARBINARY;
		else if (fieldType == Date.class)
			sqlType = Types.DATE;
		else if (fieldType == Time.class)
			sqlType = Types.TIME;
		else if (fieldType == Timestamp.class)
			sqlType = Types.TIMESTAMP;
		return sqlType;
	}
	
	/**
	 * Costruttore, che costuisce un SQLParameter con direzione IN specificandone nome e valore. Il tipo SQL verrà scelto automaticamente.
	 * @param name Nome del parametro
	 * @param value Valore del parametro
	 */
	public SQLParameter(String name, Object value)
	{
		this(name, value, _getSQLTypeFromValue(value.getClass()));
	}
	
	/**
	 * Costruttore, che costruisce un SQLParameter specificandone nome, valore e direzione. Il tipo SQL verrà scelto automaticamente.
	 * @param name Nome del parametro
	 * @param value Valore del parametro
	 * @param direction Direzione del parametro
	 */
	public SQLParameter(String name, Object value, ParameterDirection direction)
	{
		this(name, value, _getSQLTypeFromValue(value.getClass()), direction);
	}
	
	/**
	 * Costruttore, che costruisce un SQLParameter con direzione IN specificandone le proprietà
	 * @param name Nome del parametro
	 * @param value Valore del parametro
	 * @param type Tipo SQL del parametro
	 */
	public SQLParameter(String name, Object value, int type)
	{
		this(name, value, type, ParameterDirection.IN);
	}
	
	/**
	 * Costruttore, che costruisce un SQLParameter specificandone tutte le proprietà
	 * @param name Nome del parametro
	 * @param value Valore del parametro
	 * @param type Tipo SQL del parametro
	 * @param direction Direzione del parametro
	 */
	public SQLParameter(String name, Object value, int type, ParameterDirection direction)
	{
		this.Name = name;
		this.Value = value;
		this.SqlType = type;
		this.Direction = direction;
	}
}