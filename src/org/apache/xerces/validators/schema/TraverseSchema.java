
/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights 
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:  
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Xerces" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written 
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation and was
 * originally based on software copyright (c) 1999, International
 * Business Machines, Inc., http://www.apache.org.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.xerces.validators.schema;

import  org.apache.xerces.framework.XMLErrorReporter;
import  org.apache.xerces.validators.schema.SchemaSymbols;
import  org.apache.xerces.validators.schema.XUtil;
import  org.apache.xerces.validators.datatype.DatatypeValidator;
import  org.apache.xerces.utils.StringPool;
import  org.w3c.dom.Element;

//REVISIT: for now, import everything in the DOM package
import  org.w3c.dom.*;

//Unit Test 
import  org.apache.xerces.parsers.DOMParser;
import  org.xml.sax.InputSource;
import  org.xml.sax.SAXParseException;
import  org.xml.sax.EntityResolver;
import  org.xml.sax.ErrorHandler;
import  org.xml.sax.SAXException;
import  java.io.IOException;
import  org.w3c.dom.Document;
import  org.apache.xml.serialize.OutputFormat;
import  org.apache.xml.serialize.XMLSerializer;



/**
 * Instances of this class get delegated to Traverse the Schema and
 * to populate the Grammar internal representation by
 * instances of Grammar objects.
 * Traverse a Schema Grammar:
     * As of April 07, 2000 the following is the
     * XML Representation of Schemas and Schema components,
     * Chapter 4 of W3C Working Draft.
     * <schema 
     *   attributeFormDefault = qualified | unqualified 
     *   blockDefault = #all or (possibly empty) subset of {equivClass, extension, restriction} 
     *   elementFormDefault = qualified | unqualified 
     *   finalDefault = #all or (possibly empty) subset of {extension, restriction} 
     *   id = ID 
     *   targetNamespace = uriReference 
     *   version = string>
     *   Content: ((include | import | annotation)* , ((simpleType | complexType | element | group | attribute | attributeGroup | notation) , annotation*)+)
     * </schema>
     * 
     * 
     * <attribute 
     *   form = qualified | unqualified 
     *   id = ID 
     *   name = NCName 
     *   ref = QName 
     *   type = QName 
     *   use = default | fixed | optional | prohibited | required 
     *   value = string>
     *   Content: (annotation? , simpleType?)
     * </>
     * 
     * <element 
     *   abstract = boolean 
     *   block = #all or (possibly empty) subset of {equivClass, extension, restriction} 
     *   default = string 
     *   equivClass = QName 
     *   final = #all or (possibly empty) subset of {extension, restriction} 
     *   fixed = string 
     *   form = qualified | unqualified 
     *   id = ID 
     *   maxOccurs = string 
     *   minOccurs = nonNegativeInteger 
     *   name = NCName 
     *   nullable = boolean 
     *   ref = QName 
     *   type = QName>
     *   Content: (annotation? , (simpleType | complexType)? , (unique | key | keyref)*)
     * </>
     * 
     * 
     * <complexType 
     *   abstract = boolean 
     *   base = QName 
     *   block = #all or (possibly empty) subset of {extension, restriction} 
     *   content = elementOnly | empty | mixed | textOnly 
     *   derivedBy = extension | restriction 
     *   final = #all or (possibly empty) subset of {extension, restriction} 
     *   id = ID 
     *   name = NCName>
     *   Content: (annotation? , (((minExclusive | minInclusive | maxExclusive | maxInclusive | precision | scale | length | minLength | maxLength | encoding | period | duration | enumeration | pattern)* | (element | group | all | choice | sequence | any)*) , ((attribute | attributeGroup)* , anyAttribute?)))
     * </>
     * 
     * 
     * <attributeGroup 
     *   id = ID 
     *   ref = QName>
     *   Content: (annotation?)
     * </>
     * 
     * <anyAttribute 
     *   id = ID 
     *   namespace = ##any | ##other | ##local | list of {uri, ##targetNamespace}>
     *   Content: (annotation?)
     * </anyAttribute>
     * 
     * <group 
     *   id = ID 
     *   maxOccurs = string 
     *   minOccurs = nonNegativeInteger 
     *   name = NCName 
     *   ref = QName>
     *   Content: (annotation? , (element | group | all | choice | sequence | any)*)
     * </>
     * 
     * <all 
     *   id = ID 
     *   maxOccurs = string 
     *   minOccurs = nonNegativeInteger>
     *   Content: (annotation? , (element | group | choice | sequence | any)*)
     * </all>
     * 
     * <choice 
     *   id = ID 
     *   maxOccurs = string 
     *   minOccurs = nonNegativeInteger>
     *   Content: (annotation? , (element | group | choice | sequence | any)*)
     * </choice>
     * 
     * <sequence 
     *   id = ID 
     *   maxOccurs = string 
     *   minOccurs = nonNegativeInteger>
     *   Content: (annotation? , (element | group | choice | sequence | any)*)
     * </sequence>
     * 
     * 
     * <any 
     *   id = ID 
     *   maxOccurs = string 
     *   minOccurs = nonNegativeInteger 
     *   namespace = ##any | ##other | ##local | list of {uri, ##targetNamespace} 
     *   processContents = lax | skip | strict>
     *   Content: (annotation?)
     * </any>
     * 
     * <unique 
     *   id = ID 
     *   name = NCName>
     *   Content: (annotation? , (selector , field+))
     * </unique>
     * 
     * <key 
     *   id = ID 
     *   name = NCName>
     *   Content: (annotation? , (selector , field+))
     * </key>
     * 
     * <keyref 
     *   id = ID 
     *   name = NCName 
     *   refer = QName>
     *   Content: (annotation? , (selector , field+))
     * </keyref>
     * 
     * <selector>
     *   Content: XPathExprApprox : An XPath expression 
     * </selector>
     * 
     * <field>
     *   Content: XPathExprApprox : An XPath expression 
     * </field>
     * 
     * 
     * <notation 
     *   id = ID 
     *   name = NCName 
     *   public = A public identifier, per ISO 8879 
     *   system = uriReference>
     *   Content: (annotation?)
     * </notation>
     * 
     * <annotation>
     *   Content: (appinfo | documentation)*
     * </annotation>
     * 
     * <include 
     *   id = ID 
     *   schemaLocation = uriReference>
     *   Content: (annotation?)
     * </include>
     * 
     * <import 
     *   id = ID 
     *   namespace = uriReference 
     *   schemaLocation = uriReference>
     *   Content: (annotation?)
     * </import>
     * 
     * <simpleType
     *   abstract = boolean 
     *   base = QName 
     *   derivedBy = | list | restriction  : restriction
     *   id = ID 
     *   name = NCName>
     *   Content: ( annotation? , ( minExclusive | minInclusive | maxExclusive | maxInclusive | precision | scale | length | minLength | maxLength | encoding | period | duration | enumeration | pattern )* )
     * </simpleType>
     * 
     * <length
     *   id = ID 
     *   value = nonNegativeInteger>
     *   Content: ( annotation? )
     * </length>
     * 
     * <minLength
     *   id = ID 
     *   value = nonNegativeInteger>
     *   Content: ( annotation? )
     * </minLength>
     * 
     * <maxLength
     *   id = ID 
     *   value = nonNegativeInteger>
     *   Content: ( annotation? )
     * </maxLength>
     * 
     * 
     * <pattern
     *   id = ID 
     *   value = string>
     *   Content: ( annotation? )
     * </pattern>
     * 
     * 
     * <enumeration
     *   id = ID 
     *   value = string>
     *   Content: ( annotation? )
     * </enumeration>
     * 
     * <maxInclusive
     *   id = ID 
     *   value = string>
     *   Content: ( annotation? )
     * </maxInclusive>
     * 
     * <maxExclusive
     *   id = ID 
     *   value = string>
     *   Content: ( annotation? )
     * </maxExclusive>
     * 
     * <minInclusive
     *   id = ID 
     *   value = string>
     *   Content: ( annotation? )
     * </minInclusive>
     * 
     * 
     * <minExclusive
     *   id = ID 
     *   value = string>
     *   Content: ( annotation? )
     * </minExclusive>
     * 
     * <precision
     *   id = ID 
     *   value = nonNegativeInteger>
     *   Content: ( annotation? )
     * </precision>
     * 
     * <scale
     *   id = ID 
     *   value = nonNegativeInteger>
     *   Content: ( annotation? )
     * </scale>
     * 
     * <encoding
     *   id = ID 
     *   value = | hex | base64 >
     *   Content: ( annotation? )
     * </encoding>
     * 
     * 
     * <duration
     *   id = ID 
     *   value = timeDuration>
     *   Content: ( annotation? )
     * </duration>
     * 
     * <period
     *   id = ID 
     *   value = timeDuration>
     *   Content: ( annotation? )
     * </period>
     * 
 * 
 * @author Jeffrey Rodriguez
 *         Eric Ye
 * @see                  org.apache.xerces.validators.common.Grammar
 */

public class TraverseSchema {

    /**
     * 
     * @param root
     * @exception Exception
     */
    private XMLErrorReporter    fErrorReporter = null;
    private StringPool          fStringPool    = null;

    //REVISIT: fValidator needs to be initialized somehow
    private XMLValidator        fValidator     = null;

    private NodeList fGlobalGroups;
    private NodeList fGlobalAttrs;
    private NodeList fGlobalAttrGrps;

    
    private string targetNSUriString = "";

    private Hashtable fComplexTypeRegistry;

    private int fAnonTypeCount =0;
    private int fScopeCount=0;
    private int fCurrentScope=0;

    private boolean defaultQualified = false;

    class ComplexTypeInfo {
	public String typeName;
	
	public String base;
	public int derivedBy;
	public int blockSet;
	public int finalSet;

	public int scopeDefined;

	public int contentType;
	public int contentSpecHandle;
	public int attlistHead;
    }

    //REVISIT: verify the URI.
    public final static String SchemaForSchemaURI = "http://www.w3.org/TR-1/Schema";


    public  TraverseSchema(Element root ) throws Exception {
        if (root == null) { // Anything to do?
            return;
        }

	//Retrieve the targetnamespace URI information
	targetNSUriString = root.getAttribute(SchemaSymbols.ATT_TARGETNAMESPACE);
	if (targetNSUriString==null) {
	    targetNSUriString="";
	}
	targetNSURI = fStringPool.addSymbol(targetNSUriString);

	defaultQualified = 
	    root.getAttribute(SchemaSymbols.ATT_ELEMENTFORMDEFAULT).equals(SchemaSymbols.ATTVAL_QUALIFIED);

	fScopeCount++;
	fCurrentScope = 0;

	fGlobalGroups = XUtil.getChildElementsByTagNameNS(root,SchemaForSchemaURI,SchemaSymbols.ELT_GROUP);
	fGlobalAttrs  = XUtil.getChildElementsByTagNameNS(root,SchemaForSchemaURI,SchemaSymbols.ELT_ATTRIBUTE);
	fGlobalAttrGrps = XUtil.getChildElementsByTagNameNS(root,SchemaForSchemaURI,SchemaSymbols.ELT_ATTRIBUTEGROUP);


        for (Element child = XUtil.getFirstChildElement(root); child != null;
            child = XUtil.getNextSiblingElement(child)) {

            String name = child.getNodeName();

            if (name.equals(SchemaSymbols.ELT_ANNOTATION) ) {
                traverseAnnotationDecl(child);
            } else if (name.equals(SchemaSymbols.ELT_SIMPLETYPE )) {
                traverseSimpleTypeDecl(child);
            } else if (name.equals(SchemaSymbols.ELT_COMPLEXTYPE )) {
                traverseComplexTypeDecl(child);
            } else if (name.equals(SchemaSymbols.ELT_ELEMENT )) { // && child.getAttribute(SchemaSymbols.ATT_REF).equals("")) {
                traverseElementDecl(child);
            } else if (name.equals(SchemaSymbols.ELT_ATTRIBUTEGROUP)) {
                //traverseAttributeGroupDecl(child);
            } else if (name.equals( SchemaSymbols.ELT_ATTRIBUTE ) ) {
                //traverseAttributeDecl( child );
            } else if (name.equals( SchemaSymbols.ELT_WILDCARD) ) {
                traverseWildcardDecl( child);
            } else if (name.equals(SchemaSymbols.ELT_GROUP) && child.getAttribute(SchemaSymbols.ATT_REF).equals("")) {
                //traverseGroupDecl(child);
            } else if (name.equals(SchemaSymbols.ELT_NOTATION)) {
                ;
            }
        } // for each child node
    } // traverseSchema(Element)

    /**
     * No-op - Traverse Annotation Declaration
     * 
     * @param comment
     */
    private void traverseAnnotationDecl(Element comment) {
        return ;
    }

    /**
     * Traverse SimpleType declaration:
     * <simpleType
     *         abstract = boolean 
     *         base = QName 
     *         derivedBy = | list | restriction  : restriction
     *         id = ID 
     *         name = NCName>
     *         Content: ( annotation? , ( minExclusive | minInclusive | maxExclusive | maxInclusive | precision | scale | length | minLength | maxLength | encoding | period | duration | enumeration | pattern )* )
     *       </simpleType>
     * 
     * @param simpleTypeDecl
     * @return 
     */
    private int traverseSimpleTypeDecl( Element simpleTypeDecl ) {
        int simpleTypeAbstract   =  fStringPool.addSymbol(
                                                         simpleTypeDecl.getAttribute( SchemaSymbols.ATT_ABSTRACT ));

        int simpleTypeBasetype   = fStringPool.addSymbol(
                                                        simpleTypeDecl.getAttribute( SchemaSymbols.ATT_BASE ));

        int simpleTypeDerivedBy  =  fStringPool.addSymbol(
                                                         simpleTypeDecl.getAttribute( SchemaSymbols.ATT_DERIVEDBY ));

        int simpleTypeID         =  fStringPool.addSymbol(
                                                         simpleTypeDecl.getAttribute( SchemaSymbols.ATTVAL_ID ));

        int simpleTypeName       =  fStringPool.addSymbol(
                                                         simpleTypeDecl.getAttribute( SchemaSymbols.ATT_NAME ));

        Element simpleTypeChild = XUtil.getFirstChildElement(simpleTypeDecl);

        // check that base type is defined
        //REVISIT: how do we do the extension mechanism? hardwired type name?

        //DatatypeValidator baseValidator = 
       // fDatatypeRegistry.getValidatorFor(simpleTypeChild.getAttribute(ATT_NAME));
        //if (baseValidator == null) {
         //   reportSchemaError(SchemaMessageProvider.UnknownBaseDatatype,
          //                    new Object [] { simpleTypeChild.getAttribute(ATT_NAME), simpleTypeDecl.getAttribute(ATT_NAME)});

        //    return -1;
        //}

        // build facet list

        // create & register validator for "generated" type if it doesn't exist
        return -1;
    }


    /**
     * Traverse ComplexType Declaration.
     *  
     *       <complexType 
     *         abstract = boolean 
     *         base = QName 
     *         block = #all or (possibly empty) subset of {extension, restriction} 
     *         content = elementOnly | empty | mixed | textOnly 
     *         derivedBy = extension | restriction 
     *         final = #all or (possibly empty) subset of {extension, restriction} 
     *         id = ID 
     *         name = NCName>
     * 	      	Content: (annotation? , (((minExclusive | minInclusive | maxExclusive
     *                    | maxInclusive | precision | scale | length | minLength 
     *                    | maxLength | encoding | period | duration | enumeration 
     *			  | pattern)* | (element | group | all | choice | sequence | any)*) , 
     *			  ((attribute | attributeGroup)* , anyAttribute?)))
     *	      </complexType>
     * @param complexTypeDecl
     * @return 
     */
    
    //REVISIT: TO DO, base and derivation ???
    private int traverseComplexTypeDecl( Element complexTypeDecl ) { 
        int complexTypeAbstract  = fStringPool.addSymbol(
                                                        complexTypeDecl.getAttribute( SchemaSymbols.ATT_ABSTRACT ));
	String isAbstract = complexTypeDecl.getAttribute( SchemaSymbols.ATT_ABSTRACT );

        int complexTypeBase      = fStringPool.addSymbol(
                                                        complexTypeDecl.getAttribute( SchemaSymbols.ATT_BASE ));
	String base = complexTypeDecl.getAttribute(SchemaSymbols.ATT_BASE);

        int complexTypeBlock     = fStringPool.addSymbol(
                                                        complexTypeDecl.getAttribute( SchemaSymbols.ATT_BLOCK ));
	String blockSet = complexTypeDecl.getAttribute( SchemaSymbols.ATT_BLOCK );

        int complexTypeContent   = fStringPool.addSymbol(
                                                        complexTypeDecl.getAttribute( SchemaSymbols.ATT_CONTENT ));
	String content = complexTypeDecl.getAttribute(SchemaSymbols.ATT_CONTENT);

        int complexTypeDerivedBy =  fStringPool.addSymbol(
                                                         complexTypeDecl.getAttribute( SchemaSymbols.ATT_DERIVEDBY ));
	String derivedBy = complexTypeDecl.getAttribute( SchemaSymbols.ATT_DERIVEDBY );

        int complexTypeFinal     =  fStringPool.addSymbol(
                                                         complexTypeDecl.getAttribute( SchemaSymbols.ATT_FINAL ));
	String finalSet = complexTypeDecl.getAttribute( SchemaSymbols.ATT_FINAL );

        int complexTypeID        = fStringPool.addSymbol(
                                                        complexTypeDecl.getAttribute( SchemaSymbols.ATTVAL_ID ));
	String typeId = complexTypeDecl.getAttribute( SchemaSymbols.ATTVAL_ID );

        int complexTypeName      =  fStringPool.addSymbol(
                                                         complexTypeDecl.getAttribute( SchemaSymbols.ATT_NAME ));
	String typeName = complexTypeDecl.getAttribute(SchemaSymbols.ATT_NAME);	



	if (typeName.equals("")) { // gensym a unique name
	    //typeName = "http://www.apache.org/xml/xerces/internalType"+fTypeCount++;
	    typeName = "#"+fAnonTypeCount++;
	}

	int scopeDefined = fScopeCount++;
	int previousScope = fCurrentScope;
	fCurrentScope = scopeDefined;

	Element child = null;
	int contentSpecType = 0;
	int csnType = 0;
	int left = -2;
	int right = -2;
	Vector uses = new Vector();


	// skip refinement and annotations
	child = null;
	for (child = XUtil.getFirstChildElement(complexTypeDecl);
	     child != null && (child.getNodeName().equals(SchemaSymbols.ELT_MINEXCLUSIVE) ||
                               child.getNodeName().equals(SchemaSymbols.ELT_MININCLUSIVE) ||
                               child.getNodeName().equals(SchemaSymbols.ELT_MAXEXCLUSIVE) ||
			       child.getNodeName().equals(SchemaSymbols.ELT_MAXINCLUSIVE) ||
			       child.getNodeName().equals(SchemaSymbols.ELT_PRECISION) ||
			       child.getNodeName().equals(SchemaSymbols.ELT_SCALE) ||
			       child.getNodeName().equals(SchemaSymbols.ELT_LENGTH) ||
			       child.getNodeName().equals(SchemaSymbols.ELT_MINLENGTH) ||
			       child.getNodeName().equals(SchemaSymbols.ELT_MAXLENGTH) ||
			       child.getNodeName().equals(SchemaSymbols.ELT_ENCODING) ||
			       child.getNodeName().equals(SchemaSymbols.ELT_PERIOD) ||
			       child.getNodeName().equals(SchemaSymbols.ELT_DURATION) ||
			       child.getNodeName().equals(SchemaSymbols.ELT_ENUMERATION) ||
			       child.getNodeName().equals(SchemaSymbols.ELT_PATTERN) ||
			       child.getNodeName().equals(SchemaSymbols.ELT_ANNOTATION));
	     child = XUtil.getNextSiblingElement(child)) 
	{
	    //REVISIT: SimpleType restriction handling
	    if (child.getNodeName().equals(SchemaSymbols.ELT_RESTRICTIONS))
		reportSchemaError(SchemaMessageProvider.FeatureUnsupported,
				  new Object [] { "Restriction" });
	}

	    // if content = textonly, base is a datatype
	if (content.equals(SchemaSymbols.ATTVAL_TEXTONLY)) {
            if (fDatatypeRegistry.getValidatorFor(base) == null) // must be datatype
	    		reportSchemaError(SchemaMessageProvider.NotADatatype,
					  new Object [] { base }); //REVISIT check forward refs
            //handle datatypes
	    contentSpecType = fStringPool.addSymbol("DATATYPE");
	    left = fValidator.addContentSpecNode(XMLContentSpec.CONTENTSPECNODE_LEAF,
						 fStringPool.addSymbol(base),
						 -1, false);

	} 
	else {   
            contentSpecType = fStringPool.addSymbol("CHILDREN");
            csnType = XMLContentSpec.CONTENTSPECNODE_SEQ;
            boolean mixedContent = false;
	    boolean elementContent = false;
	    boolean textContent = false;
	    left = -2;
	    right = -2;
	    boolean hadContent = false;

	    if (content.equals(SchemaSymbols.ATTVAL_EMPTY)) {
		contentSpecType = fStringPool.addSymbol("EMPTY");
		left = -1; // no contentSpecNode needed
	    } else if (content.equals(SchemaSymbols.ATTVAL_MIXED) || content.equals("")) {
                contentSpecType = fStringPool.addSymbol("MIXED");
		mixedContent = true;
		csnType = XMLContentSpec.CONTENTSPECNODE_CHOICE;
	    } else if (content.equals(SchemaSymbols.ATTVAL_ELEMONLY)) {
		elementContent = true;
	    } else if (content.equals(SchemaSymbols.ATTVAL_TEXTONLY)) {
		textContent = true;
	    }

	    if (mixedContent) {
		// add #PCDATA leaf

		left = fValidator.addContentSpecNode(XMLContentSpec.CONTENTSPECNODE_LEAF,
						     -1, // -1 means "#PCDATA" is name
                                                           -1, false);
		csnType = XMLContentSpec.CONTENTSPECNODE_CHOICE;
	    }

	    for (;
		 child != null;
		 child = XUtil.getNextSiblingElement(child)) {

		int index = -2;  // to save the particle's contentSpec handle 
		hadContent = true;

		boolean seeParticle = false;

		String childName = child.getNodeName();

		if (childName.equals(SchemaSymbols.ELT_ELEMENTDECL)) {
		    //if (child.getAttribute(SchemaSymbols.ATT_REF).equals("") ) {

			if (mixedContent || elementContent) {
			    //REVISIT: unfinished
			    QName eltQName = traverseElementDecl(Child);
			    index = fValidator.addContentSpecNode( XMLContentSpec.CONTENTSPECNODE_LEAF,
								   eltQName.localpart,
								   eltQName.uri, 
								   false);
			    seeParticle = true;

			} 
			else {
			    reportSchemaError(SchemaMessageProvider.EltRefOnlyInMixedElemOnly, null);
			}

		    //} 
		    //else // REVISIT: do we really need this? or
			 // should it be done in the traverseElementDecl
			 // SchemaSymbols.ATT_REF != ""
			//index = traverseElementRef(child);

		} 
		else if (childName.equals(SchemaSymbols.ELT_GROUPDECL)) {
		    /* //if (elementContent) {
			int groupNameIndex = 0;
			if (child.getAttribute(SchemaSymbols.ATT_REF).equals("")) {
			    groupNameIndex = traverseGroup(child);
			} else
			    groupNameIndex = traverseGroupRef(child);
			index = getContentSpecHandleForElementType(groupNameIndex);
		    //} else if (!elementContent)
			//reportSchemaError(SchemaMessageProvider.OnlyInEltContent,
				//	  new Object [] { "group" });
		     */
		    index = traverseGroup(child);
		    seeParticle = true;
		  
		} 
		else if (childName.equals(SchemaSymbols.ELT_ALL)) {
		    index = traverseAll(child);
		    seeParticle = true;
		  
		} 
		else if (childName.equals(SchemaSymbols.ELT_CHOICE)) {
		    index = traverseChoice(child);
		    seeParticle = true;
		  
		} 
		else if (childName.equals(SchemaSymbols.ELT_SEQUENCE)) {
		    index = traverseSequence(child);
		    seeParticle = true;
		  
		} 
		else if (childName.equals(SchemaSymbols.ELT_ATTRIBUTEDECL) ||
			   childName.equals(SchemaSymbols.ELT_ATTRGROUPDECL)) {
		    break; // attr processing is done below
		} 
		else if (childName.equals(SchemaSymbols.ELT_ANY)) {
		    contentSpecType = fStringPool.addSymbol("ANY");
		    left = -1;
		} 
		else { // datatype qual   
		    if (complextTypeBase.equals(""))
			reportSchemaError(SchemaMessageProvider.DatatypeWithType, null);
                    else
			reportSchemaError(SchemaMessageProvider.DatatypeQualUnsupported,
					  new Object [] { childName });
		}

		// check the minOccurs and maxOccurs of the particle, and fix the  
		// contentspec accordingly
		if (seeParticle) {
		    index = expandContentModel(index, child);

		} //end of if (seeParticle)

		uses.addElement(new Integer(index));
		if (left == -2) {
		    left = index;
		} else if (right == -2) {
		    right = index;
		} else {
		    left = fValidator.addContentSpecNode(csnType, left, right, false);
		    right = index;
		}
	    } //end looping through the children

	    if (hadContent && right != -2)
                left = fValidator.addContentSpecNode(csnType, left, right, false);

	    if (mixedContent && hadContent) {
		// set occurrence count
		left = fValidator.addContentSpecNode(XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE,
						     left, -1, false);
	    }
	}
	
	// stick in ElementDeclPool as a hack
	//int typeIndex = fValidator.addElementDecl(typeNameIndex, contentSpecType, left, false);

	if (!typeName.startsWith("#")) {
	    typeName = targetNSUriString + "," + typename;
	}
	ComplexTypeInfo typeInfo = new ComplexTypeInfo();
	typeInfo.base = base;
	typeInfo.derivedBy = parseComplexDerivedby(complexTypeDecl.getAttribute(SchemaSymbols.ATT_DERIVEDBY);
	typeInfo.scopeDefined = scopedefined; 
	typeInfo.contentSpecHandle = left;
	typeInfo.contentType = contentSpecType;
	typeInfo.blockSet = parseBlockSet(complexTypeDecl.getAttribute(SchemaSymbols.ATT_BLOCK);
	typeInfo.finalSet = parseFinalSet(complexTypeDecl.getAttribute(SchemaSymbols.ATT_FINAL);

	fComplexTypeRegistry.put(typeName,typeInfo);

	for (int x = 0; x < uses.size(); x++)
	    addUse(typeNameIndex, (Integer)uses.elementAt(x));

	
	int typeNameIndex = fStringPool.addSymbol(typeName); //REVISIT namespace clashes possible
	
	// REVISIT: this part is definitely broken!!!
        // (attribute | attrGroupRef)*
	for (;
	     child != null;
	     child = XUtil.getNextSiblingElement(child)) {

	    String childName = child.getNodeName();

	    if (childName.equals(SchemaSymbols.ELT_ATTRIBUTEDECL)) {
		traverseAttributeDecl(child, typeInfo);
	    } 
	/*    else if (childName.equals(SchemaSymbols.ELT_ATTRGROUPDECL) 
		     && !child.getAttribute(SchemaSymbols.ATT_REF).equals("")) {

		int index = traverseAttrGroupRef(child);

		if (getContentSpecHandleForElementType(index) == -1) {
		    reportSchemaError(SchemaMessageProvider.FeatureUnsupported,
				      new Object [] { "Forward References to attrGroup" });
		    Vector v = null;
		    Integer i = new Integer(index);
		    if ((v = (Vector) fForwardRefs.get(i)) == null)
			v = new Vector();
		    v.addElement(new Integer(typeNameIndex));
		    fForwardRefs.put(i,v);
		    addUse(typeNameIndex, index);
		} else
		    fValidator.copyAtts(index, typeNameIndex);
	    }*/
	}

	fCurrentScope = previousScope;

	return typeNameIndex;


    } // end of method: traverseComplexTypeDecl

    int expandContentModel ( int index, Element particle) {
	
	String minOccurs = particle.getAttribute(SchemaSymbols.ATT_MINOCCURS);
	String maxOccurs = particle.getAttribute(SchemaSymbols.ATT_MINOCCURS);    

	int min, max;

	if (minOccurs.equals("")) {
	    minOccurs = "1";
	}
	if (maxOccurs.equals("") ){
	    if ( minOccurs.equals("0")) {
		maxOccurs = "1";
	    }
	    else {
		maxOccurs = minOccurs;
	    }
	}


	int leafIndex = index
	//REVISIT: !!! minoccurs, maxoccurs.
	if (minOccurs.equals("1")&& maxOccurrs.equals("1")) {

	}
	else if (minOccurs.equals("0")&& maxOccurrs.equals("1")) {
	    //zero or one
	    index = fValidator.addContentSpecNode( XMLContentSpec.CONTENTSPECNODE_ZERO_OR_ONE,
						   index,
						   -1,
						   false);
	}
	else if (minOccurs.equals("0")&& maxOccurrs.equals("unbounded")) {
	    //zero or more
	    index = fValidator.addContentSpecNode( XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE,
						   index,
						   -1,
						   false);
	}
	else if (minOccurs.equals("1")&& maxOccurrs.equals("unbounded")) {
	    //one or more
	    index = fValidator.addContentSpecNode( XMLContentSpec.CONTENTSPECNODE_ONE_OR_MORE,
						   index,
						   -1,
						   false);
	}
	else if (maxOccurs.equals("unbounded") ) {
	    // >=2 or more
	    try {
		min = Integer.parseInt(minOccurs);
	    }
	    catch (Exception e) {
		//REVISIT; error handling
		e.printStackTrace();
	    }
	    if (min<2) {
		//REVISIT: report Error here
	    }

	    // => a,a,..,a+
	    index = fValidator.addContentSpecNode( XMLContentSpec.CONTENTSPECNODE_ONE_OR_MORE,
		   index,
		   -1,
		   false);

	    for (int i=0; i < (min-1); i++) {
		index = fValidator.addContentSpecNode(XMLContentSpec.CONTENTSPECNODE_SEQ,
						      index,
						      leafindex,
						      false);
	    }

	}
	else {
	    // {n,m} => a,a,a,...(a),(a),...
	    try {
		min = Integer.parseInt(minOccurs);
		max = Integer.parseInt(maxOccurs);
	    }
	    catch {
		//REVISIT; error handling
		e.printStackTrace();
	    }
	    for (int i=0; i<(min-1); i++) {
		index = fValidator.addContentSpecNode(XMLContentSpec.CONTENTSPECNODE_SEQ,
						      index,
						      leafindex,
						      false);

	    }
	    if (max>min ) {
		int optional = fvalidator.addContentSpecNode(XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE,
							     leafindex,
							     -1,
							     false);
		for (int i=0; i < (max-min); i++) {
		    index = fValidator.addContentSpecNode(XMLContentSpec.CONTENTSPECNODE_SEQ,
							  index,
							  optional,
							  false);
		}
	    }
	}

	return index;
    }

    /**
     * Traverses Schema attribute declaration.
     *   
     *       <attribute 
     *         form = qualified | unqualified 
     *         id = ID 
     *         name = NCName 
     *         ref = QName 
     *         type = QName 
     *         use = default | fixed | optional | prohibited | required 
     *         value = string>
     *         Content: (annotation? , simpleType?)
     *       <attribute/>
     * 
     * @param attributeDecl
     * @return 
     * @exception Exception
     */
    private int traverseAttributeDecl( Element attrDecl, ComplexTypeInfo typeInfo ) throws Exception {
        int attributeForm  =  fStringPool.addSymbol(
                                                   attributeDecl.getAttribute( SchemaSymbols.ATT_FORM ));

        int attributeID    =  fStringPool.addSymbol(
                                                   attributeDecl.getAttribute( SchemaSymbols.ATTVAL_ID ));

        int attributeName  =  fStringPool.addSymbol(
                                                   attributeDecl.getAttribute( SchemaSymbols.ATT_NAME ));

        int attributeRef   =  fStringPool.addSymbol(
                                                   attributeDecl.getAttribute( SchemaSymbols.ATT_REF ));

        int attributeType  =  fStringPool.addSymbol(
                                                   attributeDecl.getAttribute( SchemaSymbols.ATT_TYPE ));

        int attributeUse   =  fStringPool.addSymbol(
                                                   attributeDecl.getAttribute( SchemaSymbols.ATT_USE ));

        int attributeValue =  fStringPool.addSymbol(
                                                   attributeDecl.getAttribute( SchemaSymbols.ATT_VALUE ));

	// attribute name
	int attName = fStringPool.addSymbol(attrDecl.getAttribute(ATT_NAME));
	// form attribute
	String isQName = elementDecl.getAttribute(SchemaSymbols.ATT_EQUIVCLASS);

	// attribute type
	int attType = -1;
	int enumeration = -1;

	String datatype = attrDecl.getAttribute(ATT_TYPE);

	if (datatype.equals("")) {
	    Element child = XUtil.getFirstChildElement(attrDecl);
	    while (child != null && !child.getNodeName().equals(ELT_DATATYPEDECL))
		child = XUtil.getNextSiblingElement(child);
	    if (child != null && child.getNodeName().equals(ELT_DATATYPEDECL)) {
		attType = fStringPool.addSymbol("DATATYPE");
		enumeration = traverseSimpleTypeDecl(child);
	    } else 
		attType = fStringPool.addSymbol("CDATA");
	} else {
	    if (datatype.equals("string")) {
		attType = fStringPool.addSymbol("CDATA");
	    } else if (datatype.equals("ID")) {
		attType = fStringPool.addSymbol("ID");
	    } else if (datatype.equals("IDREF")) {
		attType = fStringPool.addSymbol("IDREF");
	    } else if (datatype.equals("IDREFS")) {
		attType = fStringPool.addSymbol("IDREFS");
	    } else if (datatype.equals("ENTITY")) {
		attType = fStringPool.addSymbol("ENTITY");
	    } else if (datatype.equals("ENTITIES")) {
		attType = fStringPool.addSymbol("ENTITIES");
	    } else if (datatype.equals("NMTOKEN")) {
		Element e = XUtil.getFirstChildElement(attrDecl, "enumeration");
		if (e == null) {
		    attType = fStringPool.addSymbol("NMTOKEN");
		} else {
		    attType = fStringPool.addSymbol("ENUMERATION");
		    enumeration = fStringPool.startStringList();
		    for (Element literal = XUtil.getFirstChildElement(e, "literal");
			 literal != null;
			 literal = XUtil.getNextSiblingElement(literal, "literal")) {
			int stringIndex = fStringPool.addSymbol(literal.getFirstChild().getNodeValue());
			fStringPool.addStringToList(enumeration, stringIndex);
		    }
		    fStringPool.finishStringList(enumeration);
		}
	    } else if (datatype.equals("NMTOKENS")) {
		attType = fStringPool.addSymbol("NMTOKENS");
	    } else if (datatype.equals(ELT_NOTATIONDECL)) {
		attType = fStringPool.addSymbol("NOTATION");
	    } else { // REVISIT: Danger: assuming all other ATTR types are datatypes
		//REVISIT check against list of validators to ensure valid type name
		attType = fStringPool.addSymbol("DATATYPE");
		enumeration = fStringPool.addSymbol(datatype);
	    }
	}

	// attribute default type
	int attDefaultType = -1;
	int attDefaultValue = -1;

	String useVal = attrDecl.getAttribute(SchemaSymbols.ATT_USE);
	boolean required = use.equals(SchemaSymbols.ATTVAL_REQUIRED);

	if (required) {
	    attDefaultType = fStringPool.addSymbol("#REQUIRED");
	} else {
	    if (use.equals(SchemaSymbols.ATTVAL_FIXED)) {
		String fixed = attrDecl.getAttribute(SchemaSymbols.ATT_VALUE);
		if (!fixed.equals("")) {
		    attDefaultType = fStringPool.addSymbol("#FIXED");
		    attDefaultValue = fStringPool.addString(fixed);
		} 
	    }
	    else if (use.equals(SchemaSymbols.ATTVAL_DEFAULT)) {
		// attribute default value
		String defaultValue = attrDecl.getAttribute(ATT_VALUE);
		if (!defaultValue.equals("")) {
		    attDefaultType = fStringPool.addSymbol("");
		    attDefaultValue = fStringPool.addString(defaultValue);
		} 
	    }
	    else if (use.equals(SchemaSymbols.ATTVAL_PROHIBITED)) {
		attDefaultType = fStringPool.addSymbol("#PROHIBITED");
		attDefaultValue = fStringPool.addString("");
	    }
	    else {
		attDefaultType = fStringPool.addSymbol("#IMPLIED");
	    }	    // check default value is valid for the datatype.
	    if (attType == fStringPool.addSymbol("DATATYPE") && attDefaultValue != -1) {
		try { // REVISIT - integrate w/ error handling
		    String type = fStringPool.toString(enumeration);
		    DatatypeValidator v = fDatatypeRegistry.getValidatorFor(type);
		    if (v != null)
			v.validate(fStringPool.toString(attDefaultValue));
		    else
			reportSchemaError(SchemaMessageProvider.NoValidatorFor,
					  new Object [] { type });
		} catch (InvalidDatatypeValueException idve) {
		    reportSchemaError(SchemaMessageProvider.IncorrectDefaultType,
				      new Object [] { attrDecl.getAttribute(ATT_NAME), idve.getMessage() });
		} catch (Exception e) {
		    e.printStackTrace();
		    System.out.println("Internal error in attribute datatype validation");
		}
	    }
	}

	int uriIndex = -1;
	if ( isQName.equals(SchemaSymbols.ATTVAL_QUALIFIED)||
	     defaultQualified || isTopLevel(attrDecl) ) {
	    uriIndex = targetNSURI;
	}

	QName attQName = new QName(-1,attrName,attrName,uriIndex);

	// add attribute to attr decl pool in fValidator, and get back the head
	typeInfo.attlistHead = fValidator.addAttDef( typeInfo.attlistHead, 
						      attQName, attType, 
						      enumeration, attDefaultType, 
						      attDefaultValue, true);

        return -1;
    }


    /**
     * Traverse element declaration:
     *  <element
     *         abstract = boolean
     *         block = #all or (possibly empty) subset of {equivClass, extension, restriction}
     *         default = string
     *         equivClass = QName
     *         final = #all or (possibly empty) subset of {extension, restriction}
     *         fixed = string
     *         form = qualified | unqualified
     *         id = ID
     *         maxOccurs = string
     *         minOccurs = nonNegativeInteger
     *         name = NCName
     *         nullable = boolean
     *         ref = QName
     *         type = QName>
     *   Content: (annotation? , (simpleType | complexType)? , (unique | key | keyref)*)
     *   </element>
     * 
     * 
     *       The following are identity-constraint definitions
     *        <unique 
     *         id = ID 
     *         name = NCName>
     *         Content: (annotation? , (selector , field+))
     *       </unique>
     *       
     *       <key 
     *         id = ID 
     *         name = NCName>
     *         Content: (annotation? , (selector , field+))
     *       </key>
     *       
     *       <keyref 
     *         id = ID 
     *         name = NCName 
     *         refer = QName>
     *         Content: (annotation? , (selector , field+))
     *       </keyref>
     *       
     *       <selector>
     *         Content: XPathExprApprox : An XPath expression 
     *       </selector>
     *       
     *       <field>
     *         Content: XPathExprApprox : An XPath expression 
     *       </field>
     *       
     * 
     * @param elementDecl
     * @return 
     * @exception Exception
     */
    private QName traverseElementDecl(Element elementDecl) throws Exception {
        int elementBlock      =  fStringPool.addSymbol(
                                                      elementDecl.getAttribute( SchemaSymbols.ATT_BLOCK ) );

        int elementDefault    =  fStringPool.addSymbol(
                                                      elementDecl.getAttribute( SchemaSymbols.ATT_DEFAULT ));

        int elementEquivClass =  fStringPool.addSymbol(
                                                      elementDecl.getAttribute( SchemaSymbols.ATT_EQUIVCLASS ));

        int elementFinal      =  fStringPool.addSymbol(
                                                      elementDecl.getAttribute( SchemaSymbols.ATT_FINAL ));

        int elementFixed      =  fStringPool.addSymbol(
                                                      elementDecl.getAttribute( SchemaSymbols.ATT_FIXED ));

        int elementForm       =  fStringPool.addSymbol(
                                                      elementDecl.getAttribute( SchemaSymbols.ATT_FORM ));

        int elementID          =  fStringPool.addSymbol(
                                                       elementDecl.getAttribute( SchemaSymbols.ATTVAL_ID ));

        int elementMaxOccurs   =  fStringPool.addSymbol(
                                                       elementDecl.getAttribute( SchemaSymbols.ATT_MAXOCCURS ));

        int elementMinOccurs  =  fStringPool.addSymbol(
                                                      elementDecl.getAttribute( SchemaSymbols.ATT_MINOCCURS ));

        int elemenName        =  fStringPool.addSymbol(
                                                      elementDecl.getAttribute( SchemaSymbols.ATT_NAME ));

        int elementNullable   =  fStringPool.addSymbol(
                                                      elementDecl.getAttribute( SchemaSymbols.ATT_NULLABLE ));

        int elementRef        =  fStringPool.addSymbol(
                                                      elementDecl.getAttribute( SchemaSymbols.ATT_REF ));

        int elementType       =  fStringPool.addSymbol(
                                                      elementDecl.getAttribute( SchemaSymbols.ATT_TYPE ));

	int contentSpecType      = -1;
	int contentSpecNodeIndex = -1;
	int typeNameIndex = -1;
	int scopeDefined = -1; //signal a error if -1 gets gets through 
	                        //cause scope can never be -1.

	String name = elementDecl.getAttribute(SchemaSymbols.ATT_NAME);
	String ref = elementDecl.getAttribute(SchemaSymbols.ATT_REF);
	String type = elementDecl.getAttribute(SchemaSymbols.ATT_TYPE);
	String minOccurs = elementDecl.getAttribute(SchemaSymbols.ATT_MINOCCURS);
	String maxOccurs = elementDecl.getAttribute(SchemaSymbols.ATT_MAXOCCURS);
	String dflt = elementDecl.getAttribute(SchemaSymbols.ATT_DEFAULT);
	String fixed = elementDecl.getAttribute(SchemaSymbols.ATT_FIXED);
	String equivClass = elementDecl.getAttribute(SchemaSymbols.ATT_EQUIVCLASS);
	// form attribute
	String isQName = elementDecl.getAttribute(SchemaSymbols.ATT_EQUIVCLASS);

        int attrCount = 0;
	if (!ref.equals("")) attrCount++;
	if (!type.equals("")) attrCount++;
		//REVISIT top level check for ref & archref
	if (attrCount > 1)
	    reportSchemaError(SchemaMessageProvider.OneOfTypeRefArchRef, null);

	if (!ref.equals("")) {
	    if (XUtil.getFirstChildElement(elementDecl) != null)
		reportSchemaError(SchemaMessageProvider.NoContentForRef, null);
	    String prefix = "";
	    String localpart = ref;
	    int colonptr = ref.indexOf(":");
	    if ( colonptr > 0) {
		prefix = ref.substring(0,colonptr);
		localpart = ref.substring(colonptr+1);
	    }
            int localpartIndex = fStringPool.addSymbol(localpart);
	    QName eltName = new QName(fStringPool.addSymbol(prefix),
				      localpartIndex,
				      fStringPool.addSymbol(ref),
				      fStringPool.addSymbol(resolvePrefixToURI(prefix) );
 	    int elementIndex = fValidator.getDeclaration(localpartIndex, 0);
	    //if not found, traverse the top level element that if referenced
	    if (elementIndex == -1 ) {
		eltName= traverseElementDecl(getTopLevelElementByName(localpart));
	    }
	    return eltName;
	}
		
	
	ComplexTypeInfo typeInfo;

	// element has a single child element, either a datatype or a type, null if primitive
	Element content = XUtil.getFirstChildElement(elementDecl);
	
	while (content != null && content.getNodeName().equals(ELT_ANNOTATION))
            content = XUtil.getNextSiblingElement(content);
	
	boolean typeSet = false;

	if (content != null) {
	    
	    String contentName = content.getNodeName();
	    
	    if (contentName.equals(SchemaSymbols.ELT_COMPLEXTYPE)) {
		
		typeNameIndex = traverseComplexTypeDecl(content);
		ComplexTypeInfo typeInfo = 
		    fComplexTypeRegistry.get(fStringPool.toString(typeNameIndex));

		contentSpecNodeIndex = typeInfo.contentSpecHandle;
		contentSpecType = typeInfo.conentType;
		scopeDefined = typeInfo.scopeDefined;

		typeSet = true;

	    } 
	    else if (contentName.equals(ELT_SIMPLETYPEDECL)) {
		//REVISIT: TO-DO, contenttype and simpletypevalidator.
		
		traverseSimpleTypeDecl(content);
		typeSet = true;
		reportSchemaError(SchemaMessageProvider.FeatureUnsupported,
				  new Object [] { "Nesting datatype declarations" });
		// contentSpecNodeIndex = traverseDatatypeDecl(content);
		// contentSpecType = fStringPool.addSymbol("DATATYPE");
	    } else if (type.equals("")) { // "ur-typed" leaf
		contentSpecType = fStringPool.addSymbol("UR_TYPE");
		// set occurrence count
		contentSpecNodeIndex = -1;
	    } else {
		System.out.println("unhandled case in TraverseElementDecl");
	    }
	} 
	if (typeSet && (type.length()>0)) {
	    reportSchemaError(SchemaMessageProvider.FeatureUnsupported,
			      new Object [] { "can have type when have a annoymous type" });
	}
	else if (!type.equals("")) { // type specified as an attribute, 
	    String prefix = "";
	    String localpart = type;
	    int colonptr = ref.indexOf(":");
	    if ( colonptr > 0) {
		prefix = type.substring(0,colonptr);
		localpart = type.substring(colonptr+1);
	    }
	    String typeURI = resolvePrefix(prefix);
	    if (!typeURI.equals(targetNSURIString)) {
		typeInfo = getTypeInfoFromNS(typeURI, localpart);
	    }
	    typeInfo = fComplexTypeRegistry.get(typeURI+","+localpart);
	    if (typeInfo == null) {
		//REVISIT try to find in the DataTypeRegistry first;
		int typeNameIndex = traverseComplexTypeDecl(getTopLevelComplexTypeByName(localpart));
		typeInfo = 
		    fComplexTypeRegistry.get(fStringPool.toString(typeNameIndex));
	    }
	    contentSpecNodeIndex = typeInfo.contentSpecHandle;
	    contentSpecType = typeInfo.conentType;
	    scopeDefined = typeInfo.scopeDefined;
   
	}

	//
	// Create element decl
	//

	int elementNameIndex     = fStringPool.addSymbol(elementDecl.getAttribute(ATT_NAME));
	int localpartIndex = elementNameIndex;
	int uriIndex = -1;
	int enclosingScope = fCurrentScope;

	if ( isQName.equals(SchemaSymbols.ATTVAL_QUALIFIED)||
	     defaultQualified || isTopLevel(elementDecl) ) {
	    uriIndex = targetNSURI;
	    enclosingScope = 0;
	}

	QName eltQName = new QName(-1,localpartIndex,elementNameIndex,uriIndex);
	// add element decl to pool
	fValidator.setCurrentScope(enclosingScope);
	int elementIndex = fValidator.addElementDecl(eltQName, scopeDefined, contentSpecType, contentSpecNodeIndex, true);
    //        System.out.println("elementIndex:"+elementIndex+" "+elementDecl.getAttribute(ATT_NAME)+" eltType:"+elementName+" contentSpecType:"+contentSpecType+
    //                           " SpecNodeIndex:"+ contentSpecNodeIndex);

	// copy up attribute decls from type object
	if (typeInfo != null) {
	    fValidator.setCurrentScope(enclosingScope);
            fValidator.copyAttsForSchema(typeInfo.attlistHead, eltQName);
	}
	else {
	    // REVISIT: should we report error from here?
	}

        return eltQName;

    }

    Element getToplevelElementByName(String name) {
    }

    Element getTopLevelComplexTypeByName(String name) {
    }

    Element getTopLevelGroupbyName(String name) {
    }

    boolean isTopLevel(Element component) {
    }

    /**
     * Traverse attributeGroup Declaration
     * 
     *   <attributeGroup
     *         id = ID
     *         ref = QName>
     *         Content: (annotation?)
     *      </>
     * 
     * @param elementDecl
     * @exception Exception
     */
    private int traverseAttributeGroupDecl( Element attributeGroupDecl ) throws Exception {
        int attributeGroupID         =  fStringPool.addSymbol(
                                                             attributeGroupDecl.getAttribute( SchemaSymbols.ATTVAL_ID ));

        int attributeGroupName      =  fStringPool.addSymbol(
                                                            attributeGroupDecl.getAttribute( SchemaSymbols.ATT_NAME ));

        return -1;
    }


    /**
     * Traverse Group Declaration.
     * 
     * <group 
     *         id = ID 
     *         maxOccurs = string 
     *         minOccurs = nonNegativeInteger 
     *         name = NCName 
     *         ref = QName>
     *   Content: (annotation? , (element | group | all | choice | sequence | any)*)
     * <group/>
     * 
     * @param elementDecl
     * @return 
     * @exception Exception
     */
    private int traverseGroupDecl( Element groupDecl ) throws Exception {
        int groupID         =  fStringPool.addSymbol(
	    groupDecl.getAttribute( SchemaSymbols.ATTVAL_ID ));

        int groupMaxOccurs  =  fStringPool.addSymbol(
	    groupDecl.getAttribute( SchemaSymbols.ATT_MAXOCCURS ));
        int groupMinOccurs  =  fStringPool.addSymbol(
	    groupDecl.getAttribute( SchemaSymbols.ATT_MINOCCURS ));

        int groupName      =  fStringPool.addSymbol(
	    groupDecl.getAttribute( SchemaSymbols.ATT_NAME ));

        int grouRef        =  fStringPool.addSymbol(
	    groupDecl.getAttribute( SchemaSymbols.ATT_REF ));

	String groupName = groupDecl.getAttribute(SchemaSymbols.ATT_NAME);
	String collection = groupDecl.getAttribute(SchemaSymbols.ATT_COLLECTION);
	String order = groupDecl.getAttribute(SchemaSymbols.ATT_ORDER);
	String ref = groupDecl.getAttribute(SchemaSymbols.ATT_REF);

	if (!ref.equals("")) {
	    if (XUtil.getFirstChildElement(groupDecl) != null)
		reportSchemaError(SchemaMessageProvider.NoContentForRef, null);
	    String prefix = "";
	    String localpart = ref;
	    int colonptr = ref.indexOf(":");
	    if ( colonptr > 0) {
		prefix = ref.substring(0,colonptr);
		localpart = ref.substring(colonptr+1);
	    }
            int localpartIndex = fStringPool.addSymbol(localpart);
	    int contentSpecIndex = traverseElementDecl(getTopLevelGroupByName(localpart));
	    
	    return contentSpecIndex;
	}

	boolean traverseElt = true; 
	if (fCurrentScope == 0) {
	    tarverseElt = false;
	}

	Element child = XUtil.getFirstChildElement(groupDecl);
	while (child != null && child.getNodeName().equals(ELT_ANNOTATION))
	    child = XUtil.getNextSiblingElement(child);

	int contentSpecType = 0;
	int csnType = 0;
	int allChildren[] = null;
	int allChildCount = 0;

	csnType = XMLContentSpec.CONTENTSPECNODE_SEQ;
	contentSpecType = fStringPool.addSymbol("CHILDREN");
	
	int left = -2;
	int right = -2;
	boolean hadContent = false;

	for (;
	     child != null;
	     child = XUtil.getNextSiblingElement(child)) {
	    int index = -2;
	    hadContent = true;

	    boolean seeParticle = false;
	    String childName = child.getNodeName();
	    if (childName.equals(ELT_ELEMENTDECL)) {
		QName eltQName = traverseElementDecl(Child);
		index = fValidator.addContentSpecNode( XMLContentSpec.CONTENTSPECNODE_LEAF,
						       eltQName.localpart,
						       eltQName.uri, 
						       false);
		seeParticle = true;

	    } 
	    else if (childName.equals(SchemaSymbols.ELT_GROUPDECL)) {
		index = traverseGroup(child);
		seeParticle = true;

	    } 
	    else if (childName.equals(SchemaSymbols.ELT_ALL)) {
		index = traverseAll(child);
		seeParticle = true;

	    } 
	    else if (childName.equals(SchemaSymbols.ELT_CHOICE)) {
		index = traverseChoice(child);
		seeParticle = true;

	    } 
	    else if (childName.equals(SchemaSymbols.ELT_SEQUENCE)) {
		index = traverseSequence(child);
		seeParticle = true;

	    } 
	    else {
		reportSchemaError(SchemaMessageProvider.GroupContentRestricted,
				  new Object [] { "group", childName });
	    }

	    if (seeParticle) {
		index = expandContentModel( index, child);
	    }
	    if (left == -2) {
		left = index;
	    } else if (right == -2) {
		right = index;
	    } else {
		left = fValidator.addContentSpecNode(csnType, left, right, false);
		right = index;
	    }
	}
	if (hadContent && right != -2)
	    left = fValidator.addContentSpecNode(csnType, left, right, false);


	return left;
    }
    
    /**
    *
    * Traverse the Sequence declaration
    * 
    * <sequence 
    *   id = ID 
    *   maxOccurs = string 
    *   minOccurs = nonNegativeInteger>
    *   Content: (annotation? , (element | group | choice | sequence | any)*)
    * </sequence>
    * 
    **/
    int traverseSequence (Element sequenceDecl) {
	    
	Element child = XUtil.getFirstChildElement(sequenceDecl);
	while (child != null && child.getNodeName().equals(ELT_ANNOTATION))
	    child = XUtil.getNextSiblingElement(child);

	int contentSpecType = 0;
	int csnType = 0;

	csnType = XMLContentSpec.CONTENTSPECNODE_SEQ;
	contentSpecType = fStringPool.addSymbol("CHILDREN");

	int left = -2;
	int right = -2;
	boolean hadContent = false;

	for (;
	     child != null;
	     child = XUtil.getNextSiblingElement(child)) {
	    int index = -2;
	    hadContent = true;

	    boolean seeParticle = false;
	    String childName = child.getNodeName();
	    if (childName.equals(ELT_ELEMENTDECL)) {
		QName eltQName = traverseElementDecl(Child);
		index = fValidator.addContentSpecNode( XMLContentSpec.CONTENTSPECNODE_LEAF,
						       eltQName.localpart,
						       eltQName.uri, 
						       false);
		seeParticle = true;

	    } 
	    else if (childName.equals(SchemaSymbols.ELT_GROUPDECL)) {
		index = traverseGroup(child);
		seeParticle = true;

	    } 
	    else if (childName.equals(SchemaSymbols.ELT_ALL)) {
		index = traverseAll(child);
		seeParticle = true;

	    } 
	    else if (childName.equals(SchemaSymbols.ELT_CHOICE)) {
		index = traverseChoice(child);
		seeParticle = true;

	    } 
	    else if (childName.equals(SchemaSymbols.ELT_SEQUENCE)) {
		index = traverseSequence(child);
		seeParticle = true;

	    } 
	    else {
		reportSchemaError(SchemaMessageProvider.GroupContentRestricted,
				  new Object [] { "group", childName });
	    }

	    if (seeParticle) {
		index = expandContentModel( index, child);
	    }
	    if (left == -2) {
		left = index;
	    } else if (right == -2) {
		right = index;
	    } else {
		left = fValidator.addContentSpecNode(csnType, left, right, false);
		right = index;
	    }
	}

	if (hadContent && right != -2)
	    left = fValidator.addContentSpecNode(csnType, left, right, false);

	return left;
    }
    
    /**
    *
    * Traverse the Sequence declaration
    * 
    * <choice
    *   id = ID 
    *   maxOccurs = string 
    *   minOccurs = nonNegativeInteger>
    *   Content: (annotation? , (element | group | choice | sequence | any)*)
    * </choice>
    * 
    **/
    int traverseChoice (Element choiceDecl) {
	    
	// REVISIT: traverseChoice, traverseSequence can be combined
	Element child = XUtil.getFirstChildElement(choiceDecl);
	while (child != null && child.getNodeName().equals(ELT_ANNOTATION))
	    child = XUtil.getNextSiblingElement(child);

	int contentSpecType = 0;
	int csnType = 0;

	csnType = XMLContentSpec.CONTENTSPECNODE_CHOICE;
	contentSpecType = fStringPool.addSymbol("CHILDREN");

	int left = -2;
	int right = -2;
	boolean hadContent = false;

	for (;
	     child != null;
	     child = XUtil.getNextSiblingElement(child)) {
	    int index = -2;
	    hadContent = true;

	    boolean seeParticle = false;
	    String childName = child.getNodeName();
	    if (childName.equals(ELT_ELEMENTDECL)) {
		QName eltQName = traverseElementDecl(Child);
		index = fValidator.addContentSpecNode( XMLContentSpec.CONTENTSPECNODE_LEAF,
						       eltQName.localpart,
						       eltQName.uri, 
						       false);
		seeParticle = true;

	    } 
	    else if (childName.equals(SchemaSymbols.ELT_GROUPDECL)) {
		index = traverseGroup(child);
		seeParticle = true;

	    } 
	    else if (childName.equals(SchemaSymbols.ELT_ALL)) {
		index = traverseAll(child);
		seeParticle = true;

	    } 
	    else if (childName.equals(SchemaSymbols.ELT_CHOICE)) {
		index = traverseChoice(child);
		seeParticle = true;

	    } 
	    else if (childName.equals(SchemaSymbols.ELT_SEQUENCE)) {
		index = traverseSequence(child);
		seeParticle = true;

	    } 
	    else {
		reportSchemaError(SchemaMessageProvider.GroupContentRestricted,
				  new Object [] { "group", childName });
	    }

	    if (seeParticle) {
		index = expandContentModel( index, child);
	    }
	    if (left == -2) {
		left = index;
	    } else if (right == -2) {
		right = index;
	    } else {
		left = fValidator.addContentSpecNode(csnType, left, right, false);
		right = index;
	    }
	}

	if (hadContent && right != -2)
	    left = fValidator.addContentSpecNode(csnType, left, right, false);

	return left;
    }
    

   /**
    * 
    * Traverse the "All" declaration
    *
    * <all 
    *   id = ID 
    *   maxOccurs = string 
    *   minOccurs = nonNegativeInteger>
    *   Content: (annotation? , (element | group | choice | sequence | any)*)
    * </all>
    * 	
    **/

    int traverseAll( Element allDecl) {


	Element child = XUtil.getFirstChildElement(allDecl);

	while (child != null && child.getNodeName().equals(ELT_ANNOTATION))
	    child = XUtil.getNextSiblingElement(child);

	int allChildren[] = null;
	int allChildCount = 0;

	int left = -2;

	for (;
	     child != null;
	     child = XUtil.getNextSiblingElement(child)) {

	    int index = -2;
	    boolean seeParticle = false;

	    String childName = child.getNodeName();

	    if (childName.equals(ELT_ELEMENTDECL)) {
		QName eltQName = traverseElementDecl(Child);
		index = fValidator.addContentSpecNode( XMLContentSpec.CONTENTSPECNODE_LEAF,
						       eltQName.localpart,
						       eltQName.uri, 
						       false);
		seeParticle = true;

	    } 
	    else if (childName.equals(SchemaSymbols.ELT_GROUPDECL)) {
		index = traverseGroup(child);
		seeParticle = true;

	    } 
	    else if (childName.equals(SchemaSymbols.ELT_ALL)) {
		index = traverseAll(child);
		seeParticle = true;

	    } 
	    else if (childName.equals(SchemaSymbols.ELT_CHOICE)) {
		    index = traverseChoice(child);
		    seeParticle = true;

	    } 
	    else if (childName.equals(SchemaSymbols.ELT_SEQUENCE)) {
		index = traverseSequence(child);
		seeParticle = true;

	    } 
	    else {
		reportSchemaError(SchemaMessageProvider.GroupContentRestricted,
				  new Object [] { "group", childName });
	    }

	    if (seeParticle) {
		index = expandContentModel( index, child);
	    }
	    allChildren[allChildCount++] = index;
	}

	left = buildAllModel(allChildren,allChildCount);

	return left;
    }
    
    /** builds the all content model */
    private int buildAllModel(int children[], int count) throws Exception {

        // build all model
        if (count > 1) {

            // create and initialize singletons
            XMLContentSpec.Node choice = new XMLContentSpec.Node();

            choice.type = XMLContentSpec.CONTENTSPECNODE_CHOICE;
            choice.value = -1;
            choice.otherValue = -1;

            // build all model
            sort(children, 0, count);
            int index = buildAllModel(children, 0, choice);

            return index;
        }

        if (count > 0) {
            return children[0];
        }

        return -1;
    }

    /** Builds the all model. */
    private int buildAllModel(int src[], int offset,
                              XMLContentSpec.Node choice) throws Exception {

        // swap last two places
        if (src.length - offset == 2) {
            int seqIndex = createSeq(src);
            if (choice.value == -1) {
                choice.value = seqIndex;
            }
            else {
                if (choice.otherValue != -1) {
                    choice.value = fValidator.addContentSpecNode(choice.type, choice.value, choice.otherValue, false);
                }
                choice.otherValue = seqIndex;
            }
            swap(src, offset, offset + 1);
            seqIndex = createSeq(src);
            if (choice.value == -1) {
                choice.value = seqIndex;
            }
            else {
                if (choice.otherValue != -1) {
                    choice.value = fValidator.addContentSpecNode(choice.type, choice.value, choice.otherValue, false);
                }
                choice.otherValue = seqIndex;
            }
            return fValidator.addContentSpecNode(choice.type, choice.value, choice.otherValue, false);
        }

        // recurse
        for (int i = offset; i < src.length - 1; i++) {
            choice.value = buildAllModel(src, offset + 1, choice);
            choice.otherValue = -1;
            sort(src, offset, src.length - offset);
            shift(src, offset, i + 1);
        }

        int choiceIndex = buildAllModel(src, offset + 1, choice);
        sort(src, offset, src.length - offset);

        return choiceIndex;

    } // buildAllModel(int[],int,ContentSpecNode,ContentSpecNode):int

    /** Creates a sequence. */
    private int createSeq(int src[]) throws Exception {

        int left = src[0];
        int right = src[1];

        for (int i = 2; i < src.length; i++) {
            left = fValidator.addContentSpecNode(XMLContentSpec.CONTENTSPECNODE_SEQ,
                                                       left, right, false);
            right = src[i];
        }

        return fValidator.addContentSpecNode(XMLContentSpec.CONTENTSPECNODE_SEQ,
                                                   left, right, false);

    } // createSeq(int[]):int

    /** Shifts a value into position. */
    private void shift(int src[], int pos, int offset) {

        int temp = src[offset];
        for (int i = offset; i > pos; i--) {
            src[i] = src[i - 1];
        }
        src[pos] = temp;

    } // shift(int[],int,int)

    /** Simple sort. */
    private void sort(int src[], final int offset, final int length) {

        for (int i = offset; i < offset + length - 1; i++) {
            int lowest = i;
            for (int j = i + 1; j < offset + length; j++) {
                if (src[j] < src[lowest]) {
                    lowest = j;
                }
            }
            if (lowest != i) {
                int temp = src[i];
                src[i] = src[lowest];
                src[lowest] = temp;
            }
        }

    } // sort(int[],int,int)

    /** Swaps two values. */
    private void swap(int src[], int i, int j) {

        int temp = src[i];
        src[i] = src[j];
        src[j] = temp;

    } // swap(int[],int,int)

    /**
     * Traverse Wildcard declaration
     * 
     * <any 
     *   id = ID 
     *   maxOccurs = string 
     *   minOccurs = nonNegativeInteger 
     *   namespace = ##any | ##other | ##local | list of {uri, ##targetNamespace} 
     *   processContents = lax | skip | strict>
     *   Content: (annotation?)
     * </any>
     * @param elementDecl
     * @return 
     * @exception Exception
     */
    private int traverseWildcardDecl( Element wildcardDecl ) throws Exception {
        int wildcardID         =  fStringPool.addSymbol(
                                                       wildcardDecl.getAttribute( SchemaSymbols.ATTVAL_ID ));

        int wildcardMaxOccurs  =  fStringPool.addSymbol(
                                                       wildcardDecl.getAttribute( SchemaSymbols.ATT_MAXOCCURS ));

        int wildcardMinOccurs  =  fStringPool.addSymbol(
                                                       wildcardDecl.getAttribute( SchemaSymbols.ATT_MINOCCURS ));

        int wildcardNamespace  =  fStringPool.addSymbol(
                                                       wildcardDecl.getAttribute( SchemaSymbols.ATT_NAMESPACE ));

        int wildcardProcessContents =  fStringPool.addSymbol(
                                                            wildcardDecl.getAttribute( SchemaSymbols.ATT_PROCESSCONTENTS ));


        int wildcardContent =  fStringPool.addSymbol(
                                                    wildcardDecl.getAttribute( SchemaSymbols.ATT_CONTENT ));


        return -1;
    }
    
    
    // utilities from Tom Watson's SchemaParser class
    // TODO: Need to make this more conformant with Schema int type parsing

    private int parseInt (String intString) throws Exception
    {
	    if ( intString.equals("*") ) {
		    return Schema.INFINITY;
	    } else {
		    return Integer.parseInt (intString);
	    }
    }

    private int parseSimpleDerivedBy (String derivedByString) throws Exception
    {
	    if ( derivedByString.equals (Schema.VAL_LIST) ) {
		    return Schema.LIST;
	    } else if ( derivedByString.equals (Schema.VAL_RESTRICTION) ) {
		    return Schema.RESTRICTION;
	    } else if ( derivedByString.equals (Schema.VAL_REPRODUCTION) ) {
		    return Schema.REPRODUCTION;
	    } else {
		    reportGenericSchemaError ("Invalid value for 'derivedBy'");
		    return -1;
	    }
    }

    private int parseComplexDerivedBy (String derivedByString)  throws Exception
    {
	    if ( derivedByString.equals (Schema.VAL_EXTENSION) ) {
		    return Schema.EXTENSION;
	    } else if ( derivedByString.equals (Schema.VAL_RESTRICTION) ) {
		    return Schema.RESTRICTION;
	    } else if ( derivedByString.equals (Schema.VAL_REPRODUCTION) ) {
		    return Schema.REPRODUCTION;
	    } else {
		    reportGenericSchemaError ( "Invalid value for 'derivedBy'" );
		    return -1;
	    }
    }

    private int parseSimpleFinal (String finalString) throws Exception
    {
	    if ( finalString.equals (Schema.VAL_POUNDALL) ) {
		    return Schema.ENUMERATION+Schema.RESTRICTION+Schema.LIST+Schema.REPRODUCTION;
	    } else {
		    int enumerate = 0;
		    int restrict = 0;
		    int list = 0;
		    int reproduce = 0;

		    StringTokenizer t = new StringTokenizer (finalString, " ");
		    while (t.hasMoreTokens()) {
			    String token = t.nextToken ();

			    if ( token.equals (Schema.VAL_ENUMERATION) ) {
				    if ( enumerate == 0 ) {
					    enumerate = Schema.ENUMERATION;
				    } else {
					    reportGenericSchemaError ("enumeration in set twice");
				    }
			    } else if ( token.equals (Schema.VAL_RESTRICTION) ) {
				    if ( restrict == 0 ) {
					    restrict = Schema.RESTRICTION;
				    } else {
					    reportGenericSchemaError ("restriction in set twice");
				    }
			    } else if ( token.equals (Schema.VAL_LIST) ) {
				    if ( list == 0 ) {
					    list = Schema.LIST;
				    } else {
					    reportGenericSchemaError ("list in set twice");
				    }
			    } else if ( token.equals (Schema.VAL_REPRODUCTION) ) {
				    if ( reproduce == 0 ) {
					    reproduce = Schema.REPRODUCTION;
				    } else {
					    reportGenericSchemaError ("reproduction in set twice");
				    }
			    } else {
					    reportGenericSchemaError (	"Invalid value (" + 
												    finalString +
												    ")" );
			    }
		    }

		    return enumerate+restrict+list+reproduce;
	    }
    }

    private int parseComplexContent (String contentString)  throws Exception
    {
	    if ( contentString.equals (Schema.VAL_EMPTY) ) {
		    return Schema.EMPTY;
	    } else if ( contentString.equals (Schema.VAL_ELEMENTONLY) ) {
		    return Schema.ELEMENT_ONLY;
	    } else if ( contentString.equals (Schema.VAL_TEXTONLY) ) {
		    return Schema.TEXT_ONLY;
	    } else if ( contentString.equals (Schema.VAL_MIXED) ) {
		    return Schema.MIXED;
	    } else {
		    reportGenericSchemaError ( "Invalid value for content" );
		    return -1;
	    }
    }

    private int parseDerivationSet (String finalString)  throws Exception
    {
	    if ( finalString.equals ("#all") ) {
		    return Schema.EXTENSION+Schema.RESTRICTION+Schema.REPRODUCTION;
	    } else {
		    int extend = 0;
		    int restrict = 0;
		    int reproduce = 0;

		    StringTokenizer t = new StringTokenizer (finalString, " ");
		    while (t.hasMoreTokens()) {
			    String token = t.nextToken ();

			    if ( token.equals (Schema.VAL_EXTENSION) ) {
				    if ( extend == 0 ) {
					    extend = Schema.EXTENSION;
				    } else {
					    reportGenericSchemaError ( "extension already in set" );
				    }
			    } else if ( token.equals (Schema.VAL_RESTRICTION) ) {
				    if ( restrict == 0 ) {
					    restrict = Schema.RESTRICTION;
				    } else {
					    reportGenericSchemaError ( "restriction already in set" );
				    }
			    } else if ( token.equals (Schema.VAL_REPRODUCTION) ) {
				    if ( reproduce == 0 ) {
					    reproduce = Schema.REPRODUCTION;
				    } else {
					    reportGenericSchemaError ( "reproduction already in set" );
				    }
			    } else {
				    reportGenericSchemaError ( "Invalid final value (" + finalString + ")" );
			    }
		    }

		    return extend+restrict+reproduce;
	    }
    }

    private int parseBlockSet (String finalString)  throws Exception
    {
	    if ( finalString.equals ("#all") ) {
		    return Schema.EQUIVCLASS+Schema.EXTENSION+Schema.LIST+Schema.RESTRICTION+Schema.REPRODUCTION;
	    } else {
		    int extend = 0;
		    int restrict = 0;
		    int reproduce = 0;

		    StringTokenizer t = new StringTokenizer (finalString, " ");
		    while (t.hasMoreTokens()) {
			    String token = t.nextToken ();

			    if ( token.equals (Schema.VAL_EQUIVCLASS) ) {
				    if ( extend == 0 ) {
					    extend = Schema.EQUIVCLASS;
				    } else {
					    reportGenericSchemaError ( "'equivClass' already in set" );
				    }
			    } else if ( token.equals (Schema.VAL_EXTENSION) ) {
				    if ( extend == 0 ) {
					    extend = Schema.EXTENSION;
				    } else {
					    reportGenericSchemaError ( "extension already in set" );
				    }
			    } else if ( token.equals (Schema.VAL_LIST) ) {
				    if ( extend == 0 ) {
					    extend = Schema.LIST;
				    } else {
					    reportGenericSchemaError ( "'list' already in set" );
				    }
			    } else if ( token.equals (Schema.VAL_RESTRICTION) ) {
				    if ( restrict == 0 ) {
					    restrict = Schema.RESTRICTION;
				    } else {
					    reportGenericSchemaError ( "restriction already in set" );
				    }
			    } else if ( token.equals (Schema.VAL_REPRODUCTION) ) {
				    if ( reproduce == 0 ) {
					    reproduce = Schema.REPRODUCTION;
				    } else {
					    reportGenericSchemaError ( "reproduction already in set" );
				    }
			    } else {
				    reportGenericSchemaError ( "Invalid final value (" + finalString + ")" );
			    }
		    }

		    return extend+restrict+reproduce;
	    }
    }


    private void reportSchemaError(int major, Object args[]) {
        try {
            fErrorReporter.reportError(fErrorReporter.getLocator(),
                                       SchemaMessageProvider.SCHEMA_DOMAIN,
                                       major,
                                       SchemaMessageProvider.MSG_NONE,
                                       args,
                                       XMLErrorReporter.ERRORTYPE_RECOVERABLE_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Unit Test here
    public static void main( String args[] ) {

        if( args.length != 1 ) {
            System.out.println( "Error: Usage java TraverseSchema yourFile.xsd" );
            System.exit(0);
        }

        DOMParser parser = new DOMParser() {
            public void ignorableWhitespace(char ch[], int start, int length) {}
            public void ignorableWhitespace(int dataIdx) {}
        };
        parser.setEntityResolver( new Resolver() );
        parser.setErrorHandler(  new ErrorHandler() );

        try {
        parser.setFeature("http://xml.org/sax/features/validation", true);
        parser.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", false);
        }catch(  org.xml.sax.SAXNotRecognizedException e ) {
            e.printStackTrace();
        }catch( org.xml.sax.SAXNotSupportedException e ) {
            e.printStackTrace();
        }

        try {
        parser.parse( args[0]);
        }catch( IOException e ) {
            e.printStackTrace();
        }catch( SAXException e ) {
            e.printStackTrace();
        }

        Document     document   = parser.getDocument(); //Our Grammar

        OutputFormat    format  = new OutputFormat( document );
        XMLSerializer    serial = new XMLSerializer( System.out, format );

        TraverseSchema tst = null;
        try {
            Element root   = document.getDocumentElement();// This is what we pass to TraverserSchema
            serial.serialize( root );
            tst = new TraverseSchema( root );
            }
            catch (Exception e) {
                e.printStackTrace(System.err);
            }
    }

    static class Resolver implements EntityResolver {
        private static final String SYSTEM[] = {
            "http://www.w3.org/TR/2000/WD-xmlschema-1-20000407/structures.dtd",
            "http://www.w3.org/TR/2000/WD-xmlschema-1-20000407/datatypes.dtd",
            "http://www.w3.org/TR/2000/WD-xmlschema-1-20000407/versionInfo.ent",
        };
        private static final String PATH[] = {
            "structures.dtd",
            "datatypes.dtd",
            "versionInfo.ent",
        };

        public InputSource resolveEntity(String publicId, String systemId)
        throws IOException {

            // looking for the schema DTDs?
            for (int i = 0; i < SYSTEM.length; i++) {
                if (systemId.equals(SYSTEM[i])) {
                    InputSource source = new InputSource(getClass().getResourceAsStream(PATH[i]));
                    source.setPublicId(publicId);
                    source.setSystemId(systemId);
                    return source;
                }
            }

            // use default resolution
            return null;

        } // resolveEntity(String,String):InputSource

    } // class Resolver

    static class ErrorHandler implements org.xml.sax.ErrorHandler {

        /** Warning. */
        public void warning(SAXParseException ex) {
            System.err.println("[Warning] "+
                               getLocationString(ex)+": "+
                               ex.getMessage());
        }

        /** Error. */
        public void error(SAXParseException ex) {
            System.err.println("[Error] "+
                               getLocationString(ex)+": "+
                               ex.getMessage());
        }

        /** Fatal error. */
        public void fatalError(SAXParseException ex) throws SAXException {
            System.err.println("[Fatal Error] "+
                               getLocationString(ex)+": "+
                               ex.getMessage());
            throw ex;
        }

        //
        // Private methods
        //

        /** Returns a string of the location. */
        private String getLocationString(SAXParseException ex) {
            StringBuffer str = new StringBuffer();

            String systemId_ = ex.getSystemId();
            if (systemId_ != null) {
                int index = systemId_.lastIndexOf('/');
                if (index != -1)
                    systemId_ = systemId_.substring(index + 1);
                str.append(systemId_);
            }
            str.append(':');
            str.append(ex.getLineNumber());
            str.append(':');
            str.append(ex.getColumnNumber());

            return str.toString();

        } // getLocationString(SAXParseException):String
    }


}





