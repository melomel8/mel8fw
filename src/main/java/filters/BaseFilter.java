package filters;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import sql.SQLParameter;
import sql.SQLQuerable;
import entities.BaseEntity;

/**
 * Classe base per i filtri di SQL di una entità
 * @author amelani
 *
 * @param <TEntity> Tipo dell'entità derivante da BaseEntity
 */
public abstract class BaseFilter<TEntity extends BaseEntity> implements SQLQuerable
{
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
		
		//recupero tutti i campi della classe (anche se considereremo soltanto quelli pubblici)
		Field[] fields = this.getClass().getFields();
		for (Field currentField : fields)
		{
			FilterFieldAttribute attributes = currentField.getAnnotation(FilterFieldAttribute.class);
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
}