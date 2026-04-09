package de.rfr.restinpeace.core.controller;

/**
 * Internal parameter binding kinds resolved during startup inspection.
 */
public enum ParameterKind {
    /** Path variable binding. */
    PATH,
    /** Query parameter binding. */
    QUERY,
    /** Header value binding. */
    HEADER,
    /** Request body binding. */
    BODY,
    /** Unsupported unannotated parameter. */
    UNANNOTATED
}
