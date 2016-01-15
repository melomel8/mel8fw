package sql;

import entities.BaseEntity;
import entities.BaseEntityList;

public class DBResponse<TEntity extends BaseEntity, TEntityList extends BaseEntityList<TEntity>> 
{
	public boolean Success;
	public String Message;
	public TEntityList Data;
}
