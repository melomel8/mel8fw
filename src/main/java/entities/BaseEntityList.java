package entities;

import java.util.ArrayList;
import java.util.Collection;

public class BaseEntityList<TEntity extends BaseEntity> extends ArrayList<TEntity>
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
