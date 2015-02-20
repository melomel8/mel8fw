package filters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import enumerations.ParameterDirection;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
/** Annotazione per marcare un campo di una classe come filtro per le stored procedures */
public @interface FilterFieldAttribute
{
	/** Nome del filtro sulla stored procedure. Se non specificato, usa il nome del campo della classe */
	public String Name() default "";
		
	/** Direzionalità del filtro. Se non specificato, il parametro viene inteso di ingresso */
	public ParameterDirection Direction() default ParameterDirection.IN;
	
	/** Tipo del parametro SQL. Viene preso in considerazione soltanto in caso di parametri di output */
	public int SqlType() default -1;	
}