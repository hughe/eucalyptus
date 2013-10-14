package com.eucalyptus.objectstorage

import com.eucalyptus.BaseException;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.groovy.runtime.typehandling.GroovyCastException

/**
 * Based on CompositeHelper
 * Converts exceptions to another message type. Both must
 * have BaseException as the base class.
 *
 */
public class WalrusExceptionProxy<T extends BaseException> {
	private static Logger LOG = Logger.getLogger( WalrusExceptionProxy.class );

	private static final def baseExceptionProps = BaseException.metaClass.properties.collect{ p -> p.name };

	/**
	 * Clones the source to dest on a property-name basis.
	 * Requires that both source and dest are not null. Will not
	 * set values to null in destination that are null in source
	 * @param source
	 * @param dest
	 * @return
	 */
	public static <O extends BaseException, I extends BaseException> O mapExcludeNulls( I source, O dest ) {
		def props = dest.metaClass.properties.collect{ p -> p.name };
		source.metaClass.properties.findAll{ it.name != "metaClass" && it.name != "class" && !baseExceptionProps.contains(it.name) && props.contains(it.name) && source[it.name] != null }.each{ sourceField ->
			LOG.debug("${source.class.simpleName}.${sourceField.name} as ${dest.class.simpleName}.${sourceField.name}=${source[sourceField.name]}");
			try {
				dest[sourceField.name]=source[sourceField.name];
			} catch(GroovyCastException e) {
				LOG.trace("Cannot cast class. ", e);
			} catch(ReadOnlyPropertyException e) {
				LOG.trace("Cannot set readonly property.",e);
			}
		}
		return dest;
	}

	/**
	 * Clones the source to dest on a property-name basis.
	 * Requires that both source and dest are not null. Will
	 * include null values in the mapping.
	 * @param source
	 * @param dest
	 * @return
	 */	
	public static <O extends BaseException, I extends BaseException> O mapWithNulls( I source, O dest ) {
		def props = dest.metaClass.properties.collect{ p -> p.name };
		source.metaClass.properties.findAll{ it.name != "metaClass" && it.name != "class" && !baseExceptionProps.contains(it.name) && props.contains(it.name) }.each{ sourceField ->
			LOG.debug("${source.class.simpleName}.${sourceField.name} as ${dest.class.simpleName}.${sourceField.name}=${source[sourceField.name]}");
			try {
				dest[sourceField.name]=source[sourceField.name];
			} catch(GroovyCastException e) {
				LOG.trace("Cannot cast class. ", e);
			} catch(ReadOnlyPropertyException e) {
				LOG.trace("Cannot set readonly property: ${sourceField.name}",e);
			}
		}
		return dest;
	}


}
