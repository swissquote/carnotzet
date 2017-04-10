package com.github.swissquote.carnotzet.core;

import java.util.List;

/**
 * An extension can modify the definition of a carnotzet environment
 * You can for example :
 *  - add/remove applications
 *  - add/remove volumes
 *  - replace entrypoint/cmd
 *  - add/remove environment variables
 */
public interface CarnotzetExtension {

	List<CarnotzetModule> apply(Carnotzet carnotzet);

}
