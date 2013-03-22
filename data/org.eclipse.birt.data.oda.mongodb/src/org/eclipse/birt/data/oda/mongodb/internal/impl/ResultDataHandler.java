/*
 *************************************************************************
 * Copyright (c) 2013 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Actuate Corporation - initial API and implementation
 *  
 *************************************************************************
 */

package org.eclipse.birt.data.oda.mongodb.internal.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bson.BSON;
import org.eclipse.birt.data.oda.mongodb.impl.MDbResultSetMetaData;
import org.eclipse.birt.data.oda.mongodb.internal.impl.MDbMetaData.DocumentsMetaData;
import org.eclipse.birt.data.oda.mongodb.internal.impl.MDbMetaData.FieldMetaData;
import org.eclipse.birt.data.oda.mongodb.nls.Messages;
import org.eclipse.datatools.connectivity.oda.OdaException;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.Bytes;
import com.mongodb.DBObject;
import com.mongodb.DBRefBase;

/**
 * Internal class delegated by MDbResultSet to
 * handle result data documents, mapping to ODA result set rows.
 */
public class ResultDataHandler
{
    private MDbResultSetMetaData m_rsMetaData;
    private List<String> m_flattenableLevelFields;          // tracks the sequence of nested levels being flattened
    private Map<String,FieldMetaData> m_intermediateFieldMDs;   // quick map to optimize lookup of metadata for intermediate level fields
    
    // cached values for each current top-level document
    private Map<String,ArrayFieldValues> m_nestedValues;    // key is top-level row, and field name of each level of nested collection being flattened
    private Map<String,DBObject> m_currentContainingDocs;   // key is top-level row, and name of a result set field; mapped to the current parent document that holds its value
    
    static final String TOP_LEVEL_PARENT = ""; //$NON-NLS-1$
    private static final DBObject NULL_VALUE_FIELD = (new BasicDBObject()).append( "NULL", Boolean.TRUE ); //$NON-NLS-1$

    private static Logger sm_logger = DriverUtil.getLogger();
    
    public ResultDataHandler( MDbResultSetMetaData resultSetMetaData )
    {
        m_rsMetaData = resultSetMetaData;
        m_flattenableLevelFields = new ArrayList<String>(3);
        m_intermediateFieldMDs = new HashMap<String,FieldMetaData>(3);
        m_nestedValues = new HashMap<String,ArrayFieldValues>(3);
        m_currentContainingDocs = new HashMap<String,DBObject>(8);

        // initialize all flattenable nested level cached field names
        initializeNestedLevels();
    }

    private void initializeNestedLevels()
    {
        // initialize top-level cached values
        String nextLevelArrayField = TOP_LEVEL_PARENT;
        addFlattenableField( nextLevelArrayField );

        // determine nested levels based on runtime metadata
        DocumentsMetaData docMD = getDocumentsMetaData();
        while( docMD != null )
        {
            nextLevelArrayField = docMD.getFlattenableFieldName();

            // check that field name exists
            if( nextLevelArrayField == null )
                break;
            String fieldSimpleName = MDbMetaData.getSimpleName( nextLevelArrayField );
            FieldMetaData fieldMD = docMD.getFieldMetaData( fieldSimpleName );
            if( fieldMD == null )   // not a valid field
                break;

            // initialize cached values for this field level 
            addFlattenableField( nextLevelArrayField );

            // look for next nested level
            docMD = fieldMD.getChildMetaData();
        }
    }

    private ArrayFieldValues addFlattenableField( String fieldName )
    {
        // create field's cached values, 
        // including m_flattenableLevelFields and m_nestedValues
        m_flattenableLevelFields.add( fieldName );
        return getOrCreateCachedFieldValues( fieldName );
    }

    private DocumentsMetaData getDocumentsMetaData()
    {
        return m_rsMetaData.getDocumentsMetaData();
    }

    private FieldMetaData getFieldMetaData( String fieldName )
    {   
        // result set metadata contains metadata for only the leaf fields defined in result set
        FieldMetaData fieldMD = m_rsMetaData.getColumnMetaData( fieldName );
        
        // look up intermediate field's metadata and cache in Map
        if( fieldMD == null )
        {
            fieldMD = m_intermediateFieldMDs.get( fieldName );
            if( fieldMD == null )   // not in cache yet
            {
                fieldMD = MDbMetaData.findFieldByFullName( fieldName, getDocumentsMetaData() );
                m_intermediateFieldMDs.put( fieldName, fieldMD );
            }
        }
        return fieldMD;
    }

    private boolean isFlattenableTopLevelScalarArrayField( FieldMetaData fieldMD )
    {
        if( fieldMD == null || fieldMD.isChildField() )    // not a top-level field
            return false;
        if( ! fieldMD.isArrayOfScalarValues() )
            return false;

        // ok to flatten only if not flattening any nested collection of documents,
        // or other top-level array field of scalar values
        return isFlattenableLevelField( fieldMD.getFullName() );        
    }

    private boolean isFlattenableLevelField( String fieldFullName )
    {
        return m_flattenableLevelFields != null &&
                m_flattenableLevelFields.contains( fieldFullName );
    }

    private boolean isFlattenableNestedField( FieldMetaData fieldMd )
    {
        if( isFlattenableLevelField( fieldMd.getFullName() ) )
            return true;
        return MDbMetaData.isFlattenableNestedField( fieldMd, getDocumentsMetaData() );
    }

    private ArrayFieldValues getOrCreateCachedFieldValues( String arrayAncestorName )
    {
        return doGetCachedFieldValues( arrayAncestorName, false );
    }

    private ArrayFieldValues getCachedFieldValues( String arrayAncestorName )
    {
        return doGetCachedFieldValues( arrayAncestorName, true );
    }

    private ArrayFieldValues doGetCachedFieldValues( String arrayAncestorName,
            boolean ifExists )
    {
        if( arrayAncestorName == null )
            return null;
        ArrayFieldValues existingValue = m_nestedValues.get( arrayAncestorName );
        if( ifExists || existingValue != null )
            return existingValue;
        
        // create a new instance under the arrayAncestorName
        return createCachedFieldValues( arrayAncestorName );
    }
    
    // create a new instance under the arrayAncestorName
    private ArrayFieldValues createCachedFieldValues( String arrayAncestorName )
    {
        ArrayFieldValues newFieldValues = new ArrayFieldValues( arrayAncestorName );
        m_nestedValues.put( arrayAncestorName, newFieldValues );
        return newFieldValues;
    }

    public Object getFieldValue( String fieldName, DBObject currentRow )
        throws OdaException
    {
        FieldMetaData fieldMD = getFieldMetaData( fieldName );
        if( fieldMD == null )
            throw new OdaException( Messages.bind( Messages.resultDataHandler_invalidFieldName, fieldName ));

        // flatten top level array of scalar values, if applicable
        if( isFlattenableTopLevelScalarArrayField( fieldMD ) )
        {
            ArrayFieldValues fieldValues = getOrCreateCachedFieldValues( fieldName );
            if( ! fieldValues.hasFieldValue( fieldName ) )
            {
                Object value = currentRow.get( fieldName );
                fieldValues.addFieldValue( fieldName, value, true );
                
                // trace logging
                if( getLogger().isLoggable( Level.FINEST ) )
                {
                    getLogger().finest( Messages.bind( ">> Cached array values for top-level field {0}:\n {1}", fieldName, value )); //$NON-NLS-1$
                }
            }
            Object flattenedValue = fieldValues.getCurrentValue( fieldName );
            return flattenedValue;
        }

        // handling other types of field or flattening nested collection of documents
        DBObject containerDoc = getContainerDocument( fieldName, fieldMD, currentRow );
        if( containerDoc == NULL_VALUE_FIELD )  // no value under the field or its intermediate parents
            return null;
        if( containerDoc == null )          // no nested parent container doc
        {
            // no flattening support; 
            // extract all values directly from current top-level document, if not already cached
            ArrayFieldValues cachedValues = getCachedFieldValues(TOP_LEVEL_PARENT);

            if( cachedValues.hasFieldValue( fieldName ) )    // field value(s) for column is cached
                return cachedValues.getFieldValue( fieldName );

            Object value = fetchFieldValues( fieldName, fieldMD, currentRow ); 
            // cache value; do not iterate over array elements, if exist
            cachedValues.addFieldValue( fieldName, value, false );

            // trace logging
            if( getLogger().isLoggable( Level.FINEST ) )
            {
                getLogger().finest( Messages.bind( ">> Cached non-flattened values for top-level field {0}:\n {1}", fieldName, value )); //$NON-NLS-1$
            }
            return value;
        }
        
        // containerDoc found is of the field being fetched
        if( isFlattenableLevelField( fieldName ) ) 
            return containerDoc;

        String fieldSimpleName = fieldMD.getSimpleName();
        Object value = null;
        try
        {
            value = containerDoc.get( fieldSimpleName );
        }
        catch( Exception ex )
        {
            getLogger().log( Level.SEVERE, 
                    Messages.bind( "Unable to get field value of {0} from document: ({1}).", fieldMD.getFullName(), containerDoc ), ex ); //$NON-NLS-1$
            throw new OdaException( ex );
        } 
        return value;
    }
    
    private DBObject getContainerDocument( String fieldFullName, FieldMetaData fieldMD, DBObject documentObj )
    {
        if( m_currentContainingDocs.containsKey( fieldFullName ) )
            return m_currentContainingDocs.get( fieldFullName );   // may have null value
        
        DBObject containingDoc = doGetContainerDocument( fieldFullName, fieldMD, documentObj, null );
        m_currentContainingDocs.put( fieldFullName, containingDoc );   // cache for repeated lookup
        return containingDoc;
    }
    
    private DBObject doGetContainerDocument( String fieldFullName, FieldMetaData fieldMD,
                        DBObject documentObj, String priorLevelName )
    {
        String[] fieldLevelNames = fieldMD != null ?
                                fieldMD.getLevelNames() :
                                MDbMetaData.splitFieldName( fieldFullName );
        if( fieldLevelNames.length == 0 )
            return documentObj;

        String firstLevelName = fieldLevelNames[0];
        String levelFullName = priorLevelName != null ?
                                priorLevelName + '.' + firstLevelName :
                                firstLevelName;

        DBObject currentContainerDoc = null;
        if( ! m_currentContainingDocs.containsKey( levelFullName ) )
        {
            FieldMetaData firstLevelMD = 
                    levelFullName.equals( fieldFullName ) && fieldMD != null ? 
                                fieldMD : 
                                getFieldMetaData( levelFullName );
            
            // trace logging
            if( getLogger().isLoggable( Level.FINEST ) )
            {
                getLogger().finest( Messages.bind( ">> FieldFullName= {0}, priorLevelName= {1},\n fieldMetaData= <{2}>,\n documentObj= {3}",  //$NON-NLS-1$
                        new Object[]{ fieldFullName, priorLevelName, fieldMD, documentObj }) );
                getLogger().finest( Messages.bind( " firstLevelName= {0}, levelFullName= {1},\n firstLevelMD= <{2}>",  //$NON-NLS-1$
                        new Object[]{ firstLevelName, levelFullName, firstLevelMD }) );
            }

            if( firstLevelMD == null )
                return documentObj;

            if( ! firstLevelMD.hasChildDocuments() )    // a leaf field
                return documentObj;

            if( ! isFlattenableNestedField( firstLevelMD ))
                return null;

            ArrayFieldValues firstLevelValues = getOrCreateCachedFieldValues( levelFullName );
            if( ! firstLevelValues.hasContainerDocs() )
            {
                Object value = documentObj.get( firstLevelName );
                DBObject firstLevelDocs = value != null ? 
                                            fetchFieldDocument( value ) : 
                                            NULL_VALUE_FIELD;
                firstLevelValues.addContainerDocs( firstLevelDocs );
                
                // trace logging
                if( getLogger().isLoggable( Level.FINEST ) )
                {
                    getLogger().finest( Messages.bind( ">> Cached container documents for {0}:\n {1}", levelFullName, firstLevelDocs )); //$NON-NLS-1$
                }
            }
            currentContainerDoc = firstLevelValues.getCurrentContainerDoc();

            // cache intermediate level current doc in handler to optimize repeated lookup
            m_currentContainingDocs.put( levelFullName, currentContainerDoc );   
        }
        currentContainerDoc = m_currentContainingDocs.get( levelFullName );   // may have null value
        
        if( currentContainerDoc == null || currentContainerDoc == NULL_VALUE_FIELD ) 
            return currentContainerDoc;
        if( fieldLevelNames.length == 1 )
             return currentContainerDoc;
        
        // handle next level child value
        String childFullName = MDbMetaData.stripParentName( fieldFullName, firstLevelName );
        return doGetContainerDocument( childFullName, null, currentContainerDoc, levelFullName );
    }

    public boolean next() throws OdaException
    {
        // iterate over the lowest level first
        for( int level = m_flattenableLevelFields.size(); level >= 1; level-- )
        {
            String levelFieldName = m_flattenableLevelFields.get( level-1 );
            clearCurrentDocsOf( levelFieldName );    // clear any current containing documents cached at this level

            ArrayFieldValues cachedFieldValues = getCachedFieldValues(levelFieldName);
            if( cachedFieldValues != null && cachedFieldValues.next() )
                return true;
            // done iterating all documents at this level;
            // clear cache before iterate to upper level doc
            cachedFieldValues.clearContainerDocs();     
        }

        // clear all cached values, before moving on to the next top-level document
        for( ArrayFieldValues nestedLevelValues : m_nestedValues.values() )
            nestedLevelValues.clear();
        if( ! m_currentContainingDocs.isEmpty() )
            m_currentContainingDocs.clear();
        return false;
    }

    private void clearCurrentDocsOf( String containerFieldName )
    {
        if( m_currentContainingDocs.isEmpty() ||              // nothing to clear
            TOP_LEVEL_PARENT.equals( containerFieldName ) )   // defer to end of iteration of top-level row in #next()
            return;     

        m_currentContainingDocs.remove( containerFieldName );

        // also clear all cached values of child fields under the containing field
        String parentPrefix = containerFieldName + '.';
        Set<String> cachedFieldNames = new HashSet<String>( m_currentContainingDocs.keySet() );
        for( String fieldName : cachedFieldNames )
        {
            if( fieldName.startsWith( parentPrefix ) )
                m_currentContainingDocs.remove( fieldName );
        }        
    }

    // Utility methods

    static DBObject fetchFieldDocument( Object fieldValue )
    {
        return fetchFieldDocument( fieldValue, BSON.UNDEFINED );
    }

    static DBObject fetchFieldDocument( Object fieldValue, byte fieldNativeDataType )
    {
        if( fieldNativeDataType == BSON.UNDEFINED )
            fieldNativeDataType = Bytes.getType( fieldValue );

        if( fieldNativeDataType == BSON.ARRAY )
        {
            if( ! (fieldValue instanceof List) )
                return null;

            // fetch nested document, if exists, for each element in array
            BasicDBList dbObjsList = new BasicDBList();
            for( Object valueInList : (List<?>)fieldValue )
            {
                DBObject listElementObj = fetchFieldDocument( valueInList );
                if( listElementObj == null )   // at least one element in array is not a nested doc
                    return null;
                if( listElementObj instanceof List )
                    dbObjsList.addAll( (List<?>)listElementObj );   // collapse into the same list
                else
                    dbObjsList.add( listElementObj );
            }
            return dbObjsList;  // return nested documents in an array
        }

        DBObject fieldObjValue = null;
        if( fieldNativeDataType == BSON.OBJECT )
        {
            if( fieldValue instanceof DBObject )
                fieldObjValue = (DBObject)fieldValue;
            else if( fieldValue instanceof DBRefBase )
            {
                try
                {
                    fieldObjValue = ((DBRefBase)fieldValue).fetch();
                }
                catch( Exception ex )
                {
                    // log and ignore
                    getLogger().log( Level.INFO, "Ignoring error in fetching a DBRefBase object." ); //$NON-NLS-1$
                }
            }
        }
        return fieldObjValue;
    }
    
    public static Object fetchFieldValues( String fieldFullName, DBObject documentObj )
    {
        return fetchFieldValues( fieldFullName, null, documentObj );
    }
    
    public static Object fetchFieldValues( String fieldFullName, FieldMetaData fieldMD, DBObject documentObj )
    {
        if( documentObj instanceof BasicDBList )
            return fetchFieldValuesFromList( fieldFullName, (BasicDBList)documentObj );

        String[] fieldLevelNames = fieldMD != null ?
                            fieldMD.getLevelNames() :
                            MDbMetaData.splitFieldName( fieldFullName );
        if( fieldLevelNames.length == 0 )
            return null;

        Object value = documentObj.get( fieldLevelNames[0] );
        if( value == null )     // no data in document under the specified field name
            return null;
        DBObject fieldDoc = fetchFieldDocument( value );

        if( fieldLevelNames.length == 1 )
             return fieldDoc != null ? fieldDoc : value;
        
        // handle next level child value
        if( fieldDoc == null )  // no nested document
        {
            // log and ignore
            getLogger().log( Level.INFO, 
                    Messages.bind( "The nested field ({0}) has no parent document.", fieldFullName )); //$NON-NLS-1$
            return value;
        }

        String childFullName = MDbMetaData.stripParentName( fieldFullName, fieldLevelNames[0] );
        return fetchFieldValues( childFullName, fieldDoc );
    }

    private static BasicDBList fetchFieldValuesFromList( String fieldFullName, BasicDBList fromDBList )
    {
        if( fromDBList == null || fromDBList.size() == 0 )
            return null;

        // get the named field value from each element in given array list
        BasicDBList fieldValuesList = new BasicDBList();
        if( fromDBList.isPartialObject() )
            fieldValuesList.markAsPartialObject();

        for( int index=0; index < fromDBList.size(); index++ )
        {
            Object listElementObj = fromDBList.get( String.valueOf(index) );
            if( listElementObj instanceof DBObject )    // nested complex object, e.g. document
                listElementObj = fetchFieldValues( fieldFullName, (DBObject)listElementObj );
            fieldValuesList.put( index, listElementObj );
        }

        // check if at least one field value in list is not null, return the list
        for( Object elementValue : fieldValuesList.toMap().values() )
        {
            if( elementValue != null )
                return fieldValuesList;
        }
        
        return null;    // all values in list is null
    }

    private static Logger getLogger()
    {
        return sm_logger;
    }

}