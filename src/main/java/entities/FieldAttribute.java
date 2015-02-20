package entities;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import enumerations.SQLDirection;

/**
 * Annotazione che specifica la mappatura dei campi di una classe con i campi di una tabella del db
 * @author amelani
 *
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldAttribute 
{	
	/** Nome del campo sul db. Di default è il nome del campo della classe */
	public String ParameterName() default "";
		
	/** Direzionalità del parametro. Di default è di ingresso */
	public SQLDirection Direction() default SQLDirection.IN;
	
	/** Tipo del parametro SQL. Viene preso in considerazione soltanto in caso di parametri di output */
	public int SqlType() default -1;	
}
