package entities;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotazione che specifica la mappatura della classe sulla base di dati
 * @author amelani
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EntityAttribute 
{
	/** Nome dell'entità sulla base di dati. Di default è il nome dell'entità stessa */
	public String Name() default "";
}