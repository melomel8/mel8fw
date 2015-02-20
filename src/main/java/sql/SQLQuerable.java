package sql;

import java.util.ArrayList;

/**
 * Interfaccia per le classi che possono essere inserite direttamente nelle procedure SQL
 * @author amelani
 *
 */
public interface SQLQuerable 
{
	/**
	 * Restituisce un ArrayList di parametri SQL recuperati dai valori delle proprietà della classe
	 * @return ArrayList<SQLParameters> con i parametri SQL
	 * @throws IllegalArgumentException Se la proprietà che si sta tentando di accedere non appartiene alla classe
	 * @throws IllegalAccessException Se la proprietà della classe non è accessibile
	 */
	public ArrayList<SQLParameter> GetParameters() throws IllegalArgumentException, IllegalAccessException;
}