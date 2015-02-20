package entities;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Classe base per l'implementazione delle liste di entità
 * @author amelani
 *
 * @param <TEntity> Tipo di entità derivata da BaseEntity
 */
public abstract class BaseEntityList<TEntity extends BaseEntity> extends ArrayList<TEntity>
{
	private static final long serialVersionUID = 2671759023031510688L;
	
	public BaseEntityList()
	{
		
	}
	
	public BaseEntityList(Collection<TEntity> collection)
	{
		super.addAll(collection);
	}
}