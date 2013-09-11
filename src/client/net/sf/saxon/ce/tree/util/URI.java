package client.net.sf.saxon.ce.tree.util;

import java.io.IOException;
import java.io.Serializable;

/**********************************************************************
* A class to represent a Uniform Resource Identifier (URI). This class
* is designed to handle the parsing of URIs and provide access to
* the various components (scheme, host, port, userinfo, path, query
* string and fragment) that may constitute a URI.
* <p>
* Parsing of a URI specification is done according to the URI
* syntax described in 
* <a href="http://www.ietf.org/rfc/rfc2396.txt?number=2396">RFC 2396</a>,
* and amended by
* <a href="http://www.ietf.org/rfc/rfc2732.txt?number=2732">RFC 2732</a>. 
* <p>
* Every absolute URI consists of a scheme, followed by a colon (':'), 
* followed by a scheme-specific part. For URIs that follow the 
* "generic URI" syntax, the scheme-specific part begins with two 
* slashes ("//") and may be followed by an authority segment (comprised 
* of user information, host, and port), path segment, query segment 
* and fragment. Note that RFC 2396 no longer specifies the use of the 
* parameters segment and excludes the "user:password" syntax as part of 
* the authority segment. If "user:password" appears in a URI, the entire 
* user/password string is stored as userinfo.
* <p>
* For URIs that do not follow the "generic URI" syntax (e.g. mailto),
* the entire scheme-specific part is treated as the "path" portion
* of the URI.
* <p>
* Note that, unlike the java.net.URL class, this class does not provide
* any built-in network access functionality nor does it provide any
* scheme-specific functionality (for example, it does not know a
* default port for a specific scheme). Rather, it only knows the
* grammar and basic set of operations that can be applied to a URI.
*
* @version  $Id: URI.java 447241 2006-09-18 05:12:57Z mrglavas $
*
**********************************************************************/
 public class URI implements Serializable {

  /*******************************************************************
  * MalformedURIExceptions are thrown in the process of building a URI
  * or setting fields on a URI when an operation would result in an
  * invalid URI specification.
  *
  ********************************************************************/
  public static class URISyntaxException extends IOException {

   /** Serialization version. */
   static final long serialVersionUID = -6695054834342951930L;
   
   /******************************************************************
    * Constructs a <code>MalformedURIException</code> with no specified
    * detail message.
    ******************************************************************/
    public URISyntaxException() {
      super();
    }

    /*****************************************************************
    * Constructs a <code>MalformedURIException</code> with the
    * specified detail message.
    *
    * @param p_msg the detail message.
    ******************************************************************/
    public URISyntaxException(String p_msg) {
      super(p_msg);
    }
  }


  private static final byte [] fgLookupTable = new byte[128];
  
  /**
   * Character Classes
   */
  
  /** reserved characters ;/?:@&=+$,[] */
  //RFC 2732 added '[' and ']' as reserved characters
  private static final int RESERVED_CHARACTERS = 0x01;
  
  /** URI punctuation mark characters: -_.!~*'() - these, combined with
      alphanumerics, constitute the "unreserved" characters */
  private static final int MARK_CHARACTERS = 0x02;
  
  /** scheme can be composed of alphanumerics and these characters: +-. */
  private static final int SCHEME_CHARACTERS = 0x04;
  
  /** userinfo can be composed of unreserved, escaped and these
      characters: ;:&=+$, */
  private static final int USERINFO_CHARACTERS = 0x08;
  
  /** ASCII letter characters */
  private static final int ASCII_ALPHA_CHARACTERS = 0x10;
  
  /** ASCII digit characters */
  private static final int ASCII_DIGIT_CHARACTERS = 0x20;
  
  /** ASCII hex characters */
  private static final int ASCII_HEX_CHARACTERS = 0x40;
  
  /** Path characters */
  private static final int PATH_CHARACTERS = 0x80;

  /** Mask for alpha-numeric characters */
  private static final int MASK_ALPHA_NUMERIC = ASCII_ALPHA_CHARACTERS | ASCII_DIGIT_CHARACTERS;
  
  /** Mask for unreserved characters */
  private static final int MASK_UNRESERVED_MASK = MASK_ALPHA_NUMERIC | MARK_CHARACTERS;
  
  /** Mask for URI allowable characters except for % */
  private static final int MASK_URI_CHARACTER = MASK_UNRESERVED_MASK | RESERVED_CHARACTERS;
  
  /** Mask for scheme characters */
  private static final int MASK_SCHEME_CHARACTER = MASK_ALPHA_NUMERIC | SCHEME_CHARACTERS;
  
  /** Mask for userinfo characters */
  private static final int MASK_USERINFO_CHARACTER = MASK_UNRESERVED_MASK | USERINFO_CHARACTERS;
  
  /** Mask for path characters */
  private static final int MASK_PATH_CHARACTER = MASK_UNRESERVED_MASK | PATH_CHARACTERS; 

  static {
      // Add ASCII Digits and ASCII Hex Numbers
      for (int i = '0'; i <= '9'; ++i) {
          fgLookupTable[i] |= ASCII_DIGIT_CHARACTERS | ASCII_HEX_CHARACTERS;
      }

      // Add ASCII Letters and ASCII Hex Numbers
      for (int i = 'A'; i <= 'F'; ++i) {
          fgLookupTable[i] |= ASCII_ALPHA_CHARACTERS | ASCII_HEX_CHARACTERS;
          fgLookupTable[i+0x00000020] |= ASCII_ALPHA_CHARACTERS | ASCII_HEX_CHARACTERS;
      }

      // Add ASCII Letters
      for (int i = 'G'; i <= 'Z'; ++i) {
          fgLookupTable[i] |= ASCII_ALPHA_CHARACTERS;
          fgLookupTable[i+0x00000020] |= ASCII_ALPHA_CHARACTERS;
      }

      // Add Reserved Characters
      final String reserved = ";/?:@&=+$,[]";
      for (int i=0; i<reserved.length(); i++) {
          fgLookupTable[reserved.charAt(i)] |= RESERVED_CHARACTERS;
      }

      // Add Mark Characters
      final String mark = "-_.!~*'()";
      for (int i=0; i<mark.length(); i++) {
          fgLookupTable[mark.charAt(i)] |= MARK_CHARACTERS;
      }

      // Add Scheme Characters
      fgLookupTable['+'] |= SCHEME_CHARACTERS;
      fgLookupTable['-'] |= SCHEME_CHARACTERS;
      fgLookupTable['.'] |= SCHEME_CHARACTERS;

      // Add Userinfo Characters
      final String userinfo = ";:&=+$,";
      for (int i=0; i<userinfo.length(); i++) {
          fgLookupTable[userinfo.charAt(i)] |= USERINFO_CHARACTERS;
      }

      
      // Add Path Characters
      final String path = ";/:@&=+$,";
      for (int i=0; i<path.length(); i++) {
          fgLookupTable[path.charAt(i)] |= PATH_CHARACTERS;
      }

  }

  /** Stores the scheme (usually the protocol) for this URI. */
  private String m_scheme = null;

  /** If specified, stores the userinfo for this URI; otherwise null */
  private String m_userinfo = null;

  /** If specified, stores the host for this URI; otherwise null */
  private String m_host = null;

  /** If specified, stores the port for this URI; otherwise -1 */
  private int m_port = -1;
  
  /** If specified, stores the registry based authority for this URI; otherwise -1 */
  private String m_regAuthority = null;

  /** If specified, stores the path for this URI; otherwise null */
  private String m_path = null;

  /** If specified, stores the query string for this URI; otherwise
      null.  */
  private String m_queryString = null;

  /** If specified, stores the fragment for this URI; otherwise null */
  private String m_fragment = null;

  private static boolean DEBUG = false;

  /**
  * Construct a new and uninitialized URI.
  */
  public URI() {
  }

 /**
  * Construct a new URI from another URI. All fields for this URI are
  * set equal to the fields of the URI passed in.
  *
  * @param p_other the URI to copy (cannot be null)
  */
  public URI(URI p_other) {
    initialize(p_other);
  }

 /**
  * Construct a new URI from a URI specification string. If the
  * specification follows the "generic URI" syntax, (two slashes
  * following the first colon), the specification will be parsed
  * accordingly - setting the scheme, userinfo, host,port, path, query
  * string and fragment fields as necessary. If the specification does
  * not follow the "generic URI" syntax, the specification is parsed
  * into a scheme and scheme-specific part (stored as the path) only.
  *
  * @param p_uriSpec the URI specification string (cannot be null or
  *                  empty)
  *
  * @exception URISyntaxException if p_uriSpec violates any syntax
  *                                   rules
  */
  public URI(String p_uriSpec) throws URISyntaxException {
    this((URI)null, p_uriSpec);
  }
  
  /**
   * Construct a new URI from a URI specification string. If the
   * specification follows the "generic URI" syntax, (two slashes
   * following the first colon), the specification will be parsed
   * accordingly - setting the scheme, userinfo, host,port, path, query
   * string and fragment fields as necessary. If the specification does
   * not follow the "generic URI" syntax, the specification is parsed
   * into a scheme and scheme-specific part (stored as the path) only.
   * Construct a relative URI if boolean is assigned to "true"
   * and p_uriSpec is not valid absolute URI, instead of throwing an exception. 
   * 
   * @param p_uriSpec the URI specification string (cannot be null or
   *                  empty)
   * @param allowNonAbsoluteURI true to permit non-absolute URIs, 
   *                            false otherwise.
   *
   * @exception URISyntaxException if p_uriSpec violates any syntax
   *                                   rules
   */
  public URI(String p_uriSpec, boolean allowNonAbsoluteURI) throws URISyntaxException {
      this((URI)null, p_uriSpec, allowNonAbsoluteURI);
  }
  
 /**
  * Construct a new URI from a base URI and a URI specification string.
  * The URI specification string may be a relative URI.
  *
  * @param p_base the base URI (cannot be null if p_uriSpec is null or
  *               empty)
  * @param p_uriSpec the URI specification string (cannot be null or
  *                  empty if p_base is null)
  *
  * @exception URISyntaxException if p_uriSpec violates any syntax
  *                                  rules
  */
  public URI(URI p_base, String p_uriSpec) throws URISyntaxException {
    initialize(p_base, p_uriSpec);
  }
  
  /**
   * Construct a new URI from a base URI and a URI specification string.
   * The URI specification string may be a relative URI.
   * Construct a relative URI if boolean is assigned to "true"
   * and p_uriSpec is not valid absolute URI and p_base is null
   * instead of throwing an exception. 
   *
   * @param p_base the base URI (cannot be null if p_uriSpec is null or
   *               empty)
   * @param p_uriSpec the URI specification string (cannot be null or
   *                  empty if p_base is null)
   * @param allowNonAbsoluteURI true to permit non-absolute URIs, 
   *                            false otherwise.
   *
   * @exception URISyntaxException if p_uriSpec violates any syntax
   *                                  rules
   */
  public URI(URI p_base, String p_uriSpec, boolean allowNonAbsoluteURI) throws URISyntaxException {
      initialize(p_base, p_uriSpec, allowNonAbsoluteURI);
  }

    /**
  * Initialize all fields of this URI from another URI.
  *
  * @param p_other the URI to copy (cannot be null)
  */
  private void initialize(URI p_other) {
    m_scheme = p_other.getScheme();
    m_userinfo = p_other.getUserinfo();
    m_host = p_other.getHost();
    m_port = p_other.getPort();
    m_regAuthority = p_other.getRegBasedAuthority();
    m_path = p_other.getPath();
    m_queryString = p_other.getQueryString();
    m_fragment = p_other.getFragment();
  }
  
  public URI resolve(String relative) throws URISyntaxException {
	  
	  return new URI(this, relative);
  }
  
  /**
   * Initializes this URI from a base URI and a URI specification string.
   * See RFC 2396 Section 4 and Appendix B for specifications on parsing
   * the URI and Section 5 for specifications on resolving relative URIs
   * and relative paths.
   *
   * @param p_base the base URI (may be null if p_uriSpec is an absolute
   *               URI)
   * @param p_uriSpec the URI spec string which may be an absolute or
   *                  relative URI (can only be null/empty if p_base
   *                  is not null)
   * @param allowNonAbsoluteURI true to permit non-absolute URIs, 
   *                         in case of relative URI, false otherwise.
   *
   * @exception URISyntaxException if p_base is null and p_uriSpec
   *                                  is not an absolute URI or if
   *                                  p_uriSpec violates syntax rules
   */
  private void initialize(URI p_base, String p_uriSpec, boolean allowNonAbsoluteURI)
      throws URISyntaxException {
      
      String uriSpec = p_uriSpec;
      int uriSpecLen = (uriSpec != null) ? uriSpec.length() : 0;
      
      if (p_base == null && uriSpecLen == 0) {
          if (allowNonAbsoluteURI) {
              m_path = "";
              return;
          }
          throw new URISyntaxException("Cannot initialize URI with empty parameters.");
      }
      
      // just make a copy of the base if spec is empty
      if (uriSpecLen == 0) {
          initialize(p_base);
          return;
      }
      
      int index = 0;
      
      // Check for scheme, which must be before '/', '?' or '#'.
      int colonIdx = uriSpec.indexOf(':');
      if (colonIdx != -1) {
          final int searchFrom = colonIdx - 1;
          // search backwards starting from character before ':'.
          int slashIdx = uriSpec.lastIndexOf('/', searchFrom);
          int queryIdx = uriSpec.lastIndexOf('?', searchFrom);
          int fragmentIdx = uriSpec.lastIndexOf('#', searchFrom);
          
          if (colonIdx == 0 || slashIdx != -1 || 
              queryIdx != -1 || fragmentIdx != -1) {
              // A standalone base is a valid URI according to spec
              if (colonIdx == 0 || (p_base == null && fragmentIdx != 0 && !allowNonAbsoluteURI)) {
                  throw new URISyntaxException("No scheme found in URI.");
              }
          }
          else {
              initializeScheme(uriSpec);
              index = m_scheme.length()+1;
              
              // Neither 'scheme:' or 'scheme:#fragment' are valid URIs.
              if (colonIdx == uriSpecLen - 1 || uriSpec.charAt(colonIdx+1) == '#') {
                  throw new URISyntaxException("Scheme specific part cannot be empty.");   
              }
          }
      }
      else if (p_base == null && uriSpec.indexOf('#') != 0 && !allowNonAbsoluteURI) {
          throw new URISyntaxException("No scheme found in URI.");    
      }
      
      // Two slashes means we may have authority, but definitely means we're either
      // matching net_path or abs_path. These two productions are ambiguous in that
      // every net_path (except those containing an IPv6Reference) is an abs_path. 
      // RFC 2396 resolves this ambiguity by applying a greedy left most matching rule. 
      // Try matching net_path first, and if that fails we don't have authority so 
      // then attempt to match abs_path.
      //
      // net_path = "//" authority [ abs_path ]
      // abs_path = "/"  path_segments
      if (((index+1) < uriSpecLen) &&
          (uriSpec.charAt(index) == '/' && uriSpec.charAt(index+1) == '/')) {
          index += 2;
          int startPos = index;
          
          // Authority will be everything up to path, query or fragment
          char testChar = '\0';
          while (index < uriSpecLen) {
              testChar = uriSpec.charAt(index);
              if (testChar == '/' || testChar == '?' || testChar == '#') {
                  break;
              }
              index++;
          }
          
          // Attempt to parse authority. If the section is an empty string
          // this is a valid server based authority, so set the host to this
          // value.
          if (index > startPos) {
              // If we didn't find authority we need to back up. Attempt to
              // match against abs_path next.
              if (!initializeAuthority(uriSpec.substring(startPos, index))) {
                  index = startPos - 2;
              }
          }
          else {
              m_host = "";
          }
      }
      
      initializePath(uriSpec, index);
      
      // Resolve relative URI to base URI - see RFC 2396 Section 5.2
      // In some cases, it might make more sense to throw an exception
      // (when scheme is specified is the string spec and the base URI
      // is also specified, for example), but we're just following the
      // RFC specifications
      if (p_base != null) {
          absolutize(p_base);
      }
  }

 /**
  * Initializes this URI from a base URI and a URI specification string.
  * See RFC 2396 Section 4 and Appendix B for specifications on parsing
  * the URI and Section 5 for specifications on resolving relative URIs
  * and relative paths.
  *
  * @param p_base the base URI (may be null if p_uriSpec is an absolute
  *               URI)
  * @param p_uriSpec the URI spec string which may be an absolute or
  *                  relative URI (can only be null/empty if p_base
  *                  is not null)
  *
  * @exception URISyntaxException if p_base is null and p_uriSpec
  *                                  is not an absolute URI or if
  *                                  p_uriSpec violates syntax rules
  */
  private void initialize(URI p_base, String p_uriSpec)
                         throws URISyntaxException {
	  
    String uriSpec = p_uriSpec;
    int uriSpecLen = (uriSpec != null) ? uriSpec.length() : 0;
	
    if (p_base == null && uriSpecLen == 0) {
      throw new URISyntaxException(
                  "Cannot initialize URI with empty parameters.");
    }

    // just make a copy of the base if spec is empty
    if (uriSpecLen == 0) {
      initialize(p_base);
      return;
    }

    int index = 0;

    // Check for scheme, which must be before '/', '?' or '#'.
    int colonIdx = uriSpec.indexOf(':');
    if (colonIdx != -1) {
        final int searchFrom = colonIdx - 1;
        // search backwards starting from character before ':'.
        int slashIdx = uriSpec.lastIndexOf('/', searchFrom);
        int queryIdx = uriSpec.lastIndexOf('?', searchFrom);
        int fragmentIdx = uriSpec.lastIndexOf('#', searchFrom);
       
        if (colonIdx == 0 || slashIdx != -1 || 
            queryIdx != -1 || fragmentIdx != -1) {
            // A standalone base is a valid URI according to spec
            if (colonIdx == 0 || (p_base == null && fragmentIdx != 0)) {
                throw new URISyntaxException("No scheme found in URI.");
            }
        }
        else {
            initializeScheme(uriSpec);
            index = m_scheme.length()+1;
            
            // Neither 'scheme:' or 'scheme:#fragment' are valid URIs.
            if (colonIdx == uriSpecLen - 1 || uriSpec.charAt(colonIdx+1) == '#') {
            	throw new URISyntaxException("Scheme specific part cannot be empty.");	
            }
        }
    }
    else if (p_base == null && uriSpec.indexOf('#') != 0) {
        throw new URISyntaxException("No scheme found in URI.");    
    }

    // Two slashes means we may have authority, but definitely means we're either
    // matching net_path or abs_path. These two productions are ambiguous in that
    // every net_path (except those containing an IPv6Reference) is an abs_path. 
    // RFC 2396 resolves this ambiguity by applying a greedy left most matching rule. 
    // Try matching net_path first, and if that fails we don't have authority so 
    // then attempt to match abs_path.
    //
    // net_path = "//" authority [ abs_path ]
    // abs_path = "/"  path_segments
    if (((index+1) < uriSpecLen) &&
        (uriSpec.charAt(index) == '/' && uriSpec.charAt(index+1) == '/')) {
      index += 2;
      int startPos = index;

      // Authority will be everything up to path, query or fragment
      char testChar = '\0';
      while (index < uriSpecLen) {
        testChar = uriSpec.charAt(index);
        if (testChar == '/' || testChar == '?' || testChar == '#') {
          break;
        }
        index++;
      }

      // Attempt to parse authority. If the section is an empty string
      // this is a valid server based authority, so set the host to this
      // value.
      if (index > startPos) {
        // If we didn't find authority we need to back up. Attempt to
        // match against abs_path next.
        if (!initializeAuthority(uriSpec.substring(startPos, index))) {
          index = startPos - 2;
        }
      }
      else {
        m_host = "";
      }
    }

    initializePath(uriSpec, index);

    // Resolve relative URI to base URI - see RFC 2396 Section 5.2
    // In some cases, it might make more sense to throw an exception
    // (when scheme is specified is the string spec and the base URI
    // is also specified, for example), but we're just following the
    // RFC specifications
    if (p_base != null) {
        absolutize(p_base);
    }
  }

  /**
   * Absolutize URI with given base URI.
   *
   * @param p_base base URI for absolutization
   */
  private void absolutize(URI p_base) {

      // check to see if this is the current doc - RFC 2396 5.2 #2
      // note that this is slightly different from the RFC spec in that
      // we don't include the check for query string being null
      // - this handles cases where the urispec is just a query
      // string or a fragment (e.g. "?y" or "#s") -
      // see <http://www.ics.uci.edu/~fielding/url/test1.html> which
      // identified this as a bug in the RFC
      if (m_path.length() == 0 && m_scheme == null &&
          m_host == null && m_regAuthority == null) {
          m_scheme = p_base.getScheme();
          m_userinfo = p_base.getUserinfo();
          m_host = p_base.getHost();
          m_port = p_base.getPort();
          m_regAuthority = p_base.getRegBasedAuthority();
          m_path = p_base.getPath();
          
          if (m_queryString == null) {
              m_queryString = p_base.getQueryString();
              
              if (m_fragment == null) {
                  m_fragment = p_base.getFragment();
              }
          }
          return;
      }
      
      // check for scheme - RFC 2396 5.2 #3
      // if we found a scheme, it means absolute URI, so we're done
      if (m_scheme == null) {
          m_scheme = p_base.getScheme();
      }
      else {
          return;
      }
      
      // check for authority - RFC 2396 5.2 #4
      // if we found a host, then we've got a network path, so we're done
      if (m_host == null && m_regAuthority == null) {
          m_userinfo = p_base.getUserinfo();
          m_host = p_base.getHost();
          m_port = p_base.getPort();
          m_regAuthority = p_base.getRegBasedAuthority();
      }
      else {
          return;
      }
      
      // check for absolute path - RFC 2396 5.2 #5
      if (m_path.length() > 0 &&
              m_path.startsWith("/")) {
          return;
      }
      
      // if we get to this point, we need to resolve relative path
      // RFC 2396 5.2 #6
      String path = "";
      String basePath = p_base.getPath();
      
      // 6a - get all but the last segment of the base URI path
      if (basePath != null && basePath.length() > 0) {
          int lastSlash = basePath.lastIndexOf('/');
          if (lastSlash != -1) {
              path = basePath.substring(0, lastSlash+1);
          }
      }
      else if (m_path.length() > 0) {
          path = "/";
      }
      
      // 6b - append the relative URI path
      path = path.concat(m_path);
      
      // 6c - remove all "./" where "." is a complete path segment
      int index = -1;
      while ((index = path.indexOf("/./")) != -1) {
          path = path.substring(0, index+1).concat(path.substring(index+3));
      }
      
      // 6d - remove "." if path ends with "." as a complete path segment
      if (path.endsWith("/.")) {
          path = path.substring(0, path.length()-1);
      }
      
      // 6e - remove all "<segment>/../" where "<segment>" is a complete
      // path segment not equal to ".."
      index = 1;
      int segIndex = -1;
      String tempString = null;
      
      while ((index = path.indexOf("/../", index)) > 0) {
          tempString = path.substring(0, path.indexOf("/../"));
          segIndex = tempString.lastIndexOf('/');
          if (segIndex != -1) {
              if (!tempString.substring(segIndex).equals("..")) {
                  path = path.substring(0, segIndex+1).concat(path.substring(index+4));
                  index = segIndex;
              }
              else {
                  index += 4;
              }
          }
          else {
              index += 4;
          }
      }
      
      // 6f - remove ending "<segment>/.." where "<segment>" is a
      // complete path segment
      if (path.endsWith("/..")) {
          tempString = path.substring(0, path.length()-3);
          segIndex = tempString.lastIndexOf('/');
          if (segIndex != -1) {
              path = path.substring(0, segIndex+1);
          }
      }
      m_path = path;
  }

 /**
  * Initialize the scheme for this URI from a URI string spec.
  *
  * @param p_uriSpec the URI specification (cannot be null)
  *
  * @exception URISyntaxException if URI does not have a conformant
  *                                  scheme
  */
  private void initializeScheme(String p_uriSpec)
                 throws URISyntaxException {
    int uriSpecLen = p_uriSpec.length();
    int index = 0;
    String scheme = null;
    char testChar = '\0';

    while (index < uriSpecLen) {
      testChar = p_uriSpec.charAt(index);
      if (testChar == ':' || testChar == '/' ||
          testChar == '?' || testChar == '#') {
        break;
      }
      index++;
    }
    scheme = p_uriSpec.substring(0, index);

    if (scheme.length() == 0) {
      throw new URISyntaxException("No scheme found in URI.");
    }
    else {
      setScheme(scheme);
    }
  }

 /**
  * Initialize the authority (either server or registry based)
  * for this URI from a URI string spec.
  *
  * @param p_uriSpec the URI specification (cannot be null)
  * 
  * @return true if the given string matched server or registry
  * based authority
  */
  private boolean initializeAuthority(String p_uriSpec) {
    
    int index = 0;
    int start = 0;
    int end = p_uriSpec.length();

    char testChar = '\0';
    String userinfo = null;

    // userinfo is everything up to @
    if (p_uriSpec.indexOf('@', start) != -1) {
      while (index < end) {
        testChar = p_uriSpec.charAt(index);
        if (testChar == '@') {
          break;
        }
        index++;
      }
      userinfo = p_uriSpec.substring(start, index);
      index++;
    }

    // host is everything up to last ':', or up to 
    // and including ']' if followed by ':'.
    String host = null;
    start = index;
    boolean hasPort = false;
    if (index < end) {
      if (p_uriSpec.charAt(start) == '[') {
      	int bracketIndex = p_uriSpec.indexOf(']', start);
      	index = (bracketIndex != -1) ? bracketIndex : end;
      	if (index+1 < end && p_uriSpec.charAt(index+1) == ':') {
      	  ++index;
      	  hasPort = true;
      	}
      	else {
      	  index = end;
      	}
      }
      else {
      	int colonIndex = p_uriSpec.lastIndexOf(':', end);
      	index = (colonIndex > start) ? colonIndex : end;
      	hasPort = (index != end);
      }
    }
    host = p_uriSpec.substring(start, index);
    int port = -1;
    if (host.length() > 0) {
      // port
      if (hasPort) {
        index++;
        start = index;
        while (index < end) {
          index++;
        }
        String portStr = p_uriSpec.substring(start, index);
        if (portStr.length() > 0) {
          // REVISIT: Remove this code.
          /** for (int i = 0; i < portStr.length(); i++) {
            if (!isDigit(portStr.charAt(i))) {
              throw new MalformedURIException(
                   portStr +
                   " is invalid. Port should only contain digits!");
            }
          }**/
          // REVISIT: Remove this code.
          // Store port value as string instead of integer.
          try {
            port = Integer.parseInt(portStr);
            if (port == -1) --port;
          }
          catch (NumberFormatException nfe) {
            port = -2;
          }
        }
      }
    }
    
    if (isValidServerBasedAuthority(host, port, userinfo)) {
      m_host = host;
      m_port = port;
      m_userinfo = userinfo;
      return true;
    }
    // Note: Registry based authority is being removed from a
    // new spec for URI which would obsolete RFC 2396. If the
    // spec is added to XML errata, processing of reg_name
    // needs to be removed. - mrglavas.
    else if (isValidRegistryBasedAuthority(p_uriSpec)) {
      m_regAuthority = p_uriSpec;
      return true;
    }
    return false;
  }
  
  /**
   * Determines whether the components host, port, and user info
   * are valid as a server authority.
   * 
   * @param host the host component of authority
   * @param port the port number component of authority
   * @param userinfo the user info component of authority
   * 
   * @return true if the given host, port, and userinfo compose
   * a valid server authority
   */
  private boolean isValidServerBasedAuthority(String host, int port, String userinfo) {
    
    // Check if the host is well formed.
    if (!isWellFormedAddress(host)) {
      return false;
    }
    
    // Check that port is well formed if it exists.
    // REVISIT: There's no restriction on port value ranges, but
    // perform the same check as in setPort to be consistent. Pass
    // in a string to this method instead of an integer.
    if (port < -1 || port > 65535) {
      return false;
    }
    
    // Check that userinfo is well formed if it exists.
    if (userinfo != null) {
      // Userinfo can contain alphanumerics, mark characters, escaped
      // and ';',':','&','=','+','$',','
      int index = 0;
      int end = userinfo.length();
      char testChar = '\0';
      while (index < end) {
        testChar = userinfo.charAt(index);
        if (testChar == '%') {
          if (index+2 >= end ||
            !isHex(userinfo.charAt(index+1)) ||
            !isHex(userinfo.charAt(index+2))) {
            return false;
          }
          index += 2;
        }
        else if (!isUserinfoCharacter(testChar)) {
          return false;
        }
        ++index;
      }
    }
    return true;
  }
  
  /**
   * Determines whether the given string is a registry based authority.
   * 
   * @param authority the authority component of a URI
   * 
   * @return true if the given string is a registry based authority
   */
  private boolean isValidRegistryBasedAuthority(String authority) {
    int index = 0;
    int end = authority.length();
    char testChar;
  	
    while (index < end) {
      testChar = authority.charAt(index);
      
      // check for valid escape sequence
      if (testChar == '%') {
        if (index+2 >= end ||
            !isHex(authority.charAt(index+1)) ||
            !isHex(authority.charAt(index+2))) {
            return false;
        }
        index += 2;
      }
      // can check against path characters because the set
      // is the same except for '/' which we've already excluded.
      else if (!isPathCharacter(testChar)) {
        return false;
      }
      ++index;
    }
    return true;
  }
  	
 /**
  * Initialize the path for this URI from a URI string spec.
  *
  * @param p_uriSpec the URI specification (cannot be null)
  * @param p_nStartIndex the index to begin scanning from
  *
  * @exception URISyntaxException if p_uriSpec violates syntax rules
  */
  private void initializePath(String p_uriSpec, int p_nStartIndex)
                 throws URISyntaxException {
    if (p_uriSpec == null) {
      throw new URISyntaxException(
                "Cannot initialize path from null string!");
    }

    int index = p_nStartIndex;
    int start = p_nStartIndex;
    int end = p_uriSpec.length();
    char testChar = '\0';

    // path - everything up to query string or fragment
    if (start < end) {
    	// RFC 2732 only allows '[' and ']' to appear in the opaque part.
    	if (getScheme() == null || p_uriSpec.charAt(start) == '/') {
    	
            // Scan path.
            // abs_path = "/"  path_segments
            // rel_path = rel_segment [ abs_path ]
            while (index < end) {
                testChar = p_uriSpec.charAt(index);
            
                // check for valid escape sequence
                if (testChar == '%') {
                    if (index+2 >= end ||
                    !isHex(p_uriSpec.charAt(index+1)) ||
                    !isHex(p_uriSpec.charAt(index+2))) {
                        throw new URISyntaxException(
                            "Path contains invalid escape sequence!");
                    }
                    index += 2;
                }
                // Path segments cannot contain '[' or ']' since pchar
                // production was not changed by RFC 2732.
                else if (!isPathCharacter(testChar)) {
      	            if (testChar == '?' || testChar == '#') {
      	                break;
      	            }
                    throw new URISyntaxException(
                        "Path contains invalid character: " + testChar);
                }
                ++index;
            }
        }
        else {
            
            // Scan opaque part.
            // opaque_part = uric_no_slash *uric
            while (index < end) {
                testChar = p_uriSpec.charAt(index);
            
                if (testChar == '?' || testChar == '#') {
                    break;
      	        }
                
                // check for valid escape sequence
                if (testChar == '%') {
                    if (index+2 >= end ||
                    !isHex(p_uriSpec.charAt(index+1)) ||
                    !isHex(p_uriSpec.charAt(index+2))) {
                        throw new URISyntaxException(
                            "Opaque part contains invalid escape sequence!");
                    }
                    index += 2;
                }
                // If the scheme specific part is opaque, it can contain '['
                // and ']'. uric_no_slash wasn't modified by RFC 2732, which
                // I've interpreted as an error in the spec, since the 
                // production should be equivalent to (uric - '/'), and uric
                // contains '[' and ']'. - mrglavas
                else if (!isURICharacter(testChar)) {
                    throw new URISyntaxException(
                        "Opaque part contains invalid character: " + testChar);
                }
                ++index;
            }
        }
    }
    m_path = p_uriSpec.substring(start, index);

    // query - starts with ? and up to fragment or end
    if (testChar == '?') {
      index++;
      start = index;
      while (index < end) {
        testChar = p_uriSpec.charAt(index);
        if (testChar == '#') {
          break;
        }
        if (testChar == '%') {
           if (index+2 >= end ||
              !isHex(p_uriSpec.charAt(index+1)) ||
              !isHex(p_uriSpec.charAt(index+2))) {
            throw new URISyntaxException(
                    "Query string contains invalid escape sequence!");
           }
           index += 2;
        }
        else if (!isURICharacter(testChar)) {
          throw new URISyntaxException(
                "Query string contains invalid character: " + testChar);
        }
        index++;
      }
      m_queryString = p_uriSpec.substring(start, index);
    }

    // fragment - starts with #
    if (testChar == '#') {
      index++;
      start = index;
      while (index < end) {
        testChar = p_uriSpec.charAt(index);

        if (testChar == '%') {
           if (index+2 >= end ||
              !isHex(p_uriSpec.charAt(index+1)) ||
              !isHex(p_uriSpec.charAt(index+2))) {
            throw new URISyntaxException(
                    "Fragment contains invalid escape sequence!");
           }
           index += 2;
        }
        else if (!isURICharacter(testChar)) {
          throw new URISyntaxException(
                "Fragment contains invalid character: "+testChar);
        }
        index++;
      }
      m_fragment = p_uriSpec.substring(start, index);
    }
  }

 /**
  * Get the scheme for this URI.
  *
  * @return the scheme for this URI
  */
  public String getScheme() {
    return m_scheme;
  }

 /**
  * Get the scheme-specific part for this URI (everything following the
  * scheme and the first colon). See RFC 2396 Section 5.2 for spec.
  *
  * @return the scheme-specific part for this URI
  */
  private String getSchemeSpecificPart() {
    StringBuffer schemespec = new StringBuffer();

    if (m_host != null || m_regAuthority != null) {
      schemespec.append("//");
    
      // Server based authority.
      if (m_host != null) {

        if (m_userinfo != null) {
          schemespec.append(m_userinfo);
          schemespec.append('@');
        }
        
        schemespec.append(m_host);
        
        if (m_port != -1) {
          schemespec.append(':');
          schemespec.append(m_port);
        }
      }
      // Registry based authority.
      else {
      	schemespec.append(m_regAuthority);
      }
    }

    if (m_path != null) {
      schemespec.append((m_path));
    }

    if (m_queryString != null) {
      schemespec.append('?');
      schemespec.append(m_queryString);
    }

    if (m_fragment != null) {
      schemespec.append('#');
      schemespec.append(m_fragment);
    }

    return schemespec.toString();
  }

 /**
  * Get the userinfo for this URI.
  *
  * @return the userinfo for this URI (null if not specified).
  */
  public String getUserinfo() {
    return m_userinfo;
  }

  /**
  * Get the host for this URI.
  *
  * @return the host for this URI (null if not specified).
  */
  public String getHost() {
    return m_host;
  }

 /**
  * Get the port for this URI.
  *
  * @return the port for this URI (-1 if not specified).
  */
  public int getPort() {
    return m_port;
  }
  
  /**
   * Get the registry based authority for this URI.
   * 
   * @return the registry based authority (null if not specified).
   */
  public String getRegBasedAuthority() {
    return m_regAuthority;
  }

 /**
  * Get the path for this URI. Note that the value returned is the path
  * only and does not include the query string or fragment.
  *
  * @return the path for this URI.
  */
  public String getPath() {
    return m_path;
  }

 /**
  * Get the query string for this URI.
  *
  * @return the query string for this URI. Null is returned if there
  *         was no "?" in the URI spec, empty string if there was a
  *         "?" but no query string following it.
  */
  public String getQueryString() {
    return m_queryString;
  }

 /**
  * Get the fragment for this URI.
  *
  * @return the fragment for this URI. Null is returned if there
  *         was no "#" in the URI spec, empty string if there was a
  *         "#" but no fragment following it.
  */
  public String getFragment() {
    return m_fragment;
  }

 /**
  * Set the scheme for this URI. The scheme is converted to lowercase
  * before it is set.
  *
  * @param p_scheme the scheme for this URI (cannot be null)
  *
  * @exception URISyntaxException if p_scheme is not a conformant
  *                                  scheme name
  */
  private void setScheme(String p_scheme) throws URISyntaxException {
    if (p_scheme == null) {
      throw new URISyntaxException(
                "Cannot set scheme from null string!");
    }
    if (!isConformantSchemeName(p_scheme)) {
      throw new URISyntaxException("The scheme is not conformant.");
    }

    m_scheme = p_scheme.toLowerCase();
  }

 /**
  * Determines if the passed-in Object is equivalent to this URI.
  *
  * @param p_test the Object to test for equality.
  *
  * @return true if p_test is a URI with all values equal to this
  *         URI, false otherwise
  */
  public boolean equals(Object p_test) {
    if (p_test instanceof URI) {
      URI testURI = (URI) p_test;
      if (((m_scheme == null && testURI.m_scheme == null) ||
           (m_scheme != null && testURI.m_scheme != null &&
            m_scheme.equals(testURI.m_scheme))) &&
          ((m_userinfo == null && testURI.m_userinfo == null) ||
           (m_userinfo != null && testURI.m_userinfo != null &&
            m_userinfo.equals(testURI.m_userinfo))) &&
          ((m_host == null && testURI.m_host == null) ||
           (m_host != null && testURI.m_host != null &&
            m_host.equals(testURI.m_host))) &&
            m_port == testURI.m_port &&
          ((m_path == null && testURI.m_path == null) ||
           (m_path != null && testURI.m_path != null &&
            m_path.equals(testURI.m_path))) &&
          ((m_queryString == null && testURI.m_queryString == null) ||
           (m_queryString != null && testURI.m_queryString != null &&
            m_queryString.equals(testURI.m_queryString))) &&
          ((m_fragment == null && testURI.m_fragment == null) ||
           (m_fragment != null && testURI.m_fragment != null &&
            m_fragment.equals(testURI.m_fragment)))) {
        return true;
      }
    }
    return false;
  }

 /**
  * Get the URI as a string specification. See RFC 2396 Section 5.2.
  *
  * @return the URI string specification
  */
  public String toString() {
    StringBuffer uriSpecString = new StringBuffer();

    if (m_scheme != null) {
      uriSpecString.append(m_scheme);
      uriSpecString.append(':');
    }
    uriSpecString.append(getSchemeSpecificPart());
    return uriSpecString.toString();
  }

  /**
   * Returns whether this URI represents an absolute URI.
   *
   * @return true if this URI represents an absolute URI, false
   *         otherwise
   */
  public boolean isAbsolute() {
      // presence of the scheme means absolute uri
      return (m_scheme != null);
  }

 /**
  * Determine whether a scheme conforms to the rules for a scheme name.
  * A scheme is conformant if it starts with an alphanumeric, and
  * contains only alphanumerics, '+','-' and '.'.
  *
  * @return true if the scheme is conformant, false otherwise
  */
  private static boolean isConformantSchemeName(String p_scheme) {
    if (p_scheme == null || p_scheme.trim().length() == 0) {
      return false;
    }

    if (!isAlpha(p_scheme.charAt(0))) {
      return false;
    }

    char testChar;
    int schemeLength = p_scheme.length();
    for (int i = 1; i < schemeLength; ++i) {
      testChar = p_scheme.charAt(i);
      if (!isSchemeCharacter(testChar)) {
        return false;
      }
    }

    return true;
  }

 /**
  * Determine whether a string is syntactically capable of representing
  * a valid IPv4 address, IPv6 reference or the domain name of a network host. 
  * A valid IPv4 address consists of four decimal digit groups separated by a
  * '.'. Each group must consist of one to three digits. See RFC 2732 Section 3,
  * and RFC 2373 Section 2.2, for the definition of IPv6 references. A hostname 
  * consists of domain labels (each of which must begin and end with an alphanumeric 
  * but may contain '-') separated & by a '.'. See RFC 2396 Section 3.2.2.
  *
  * @return true if the string is a syntactically valid IPv4 address, 
  * IPv6 reference or hostname
  */
  private static boolean isWellFormedAddress(String address) {
    if (address == null) {
      return false;
    }

    int addrLength = address.length();
    if (addrLength == 0) {
      return false;
    }
    
    // Check if the host is a valid IPv6reference.
    if (address.startsWith("[")) {
      return isWellFormedIPv6Reference(address);
    }

    // Cannot start with a '.', '-', or end with a '-'.
    if (address.startsWith(".") || 
        address.startsWith("-") || 
        address.endsWith("-")) {
      return false;
    }

    // rightmost domain label starting with digit indicates IP address
    // since top level domain label can only start with an alpha
    // see RFC 2396 Section 3.2.2
    int index = address.lastIndexOf('.');
    if (address.endsWith(".")) {
      index = address.substring(0, index).lastIndexOf('.');
    }

    if (index+1 < addrLength && isDigit(address.charAt(index+1))) {
      return isWellFormedIPv4Address(address);
    }
    else {
      // hostname      = *( domainlabel "." ) toplabel [ "." ]
      // domainlabel   = alphanum | alphanum *( alphanum | "-" ) alphanum
      // toplabel      = alpha | alpha *( alphanum | "-" ) alphanum
      
      // RFC 2396 states that hostnames take the form described in 
      // RFC 1034 (Section 3) and RFC 1123 (Section 2.1). According
      // to RFC 1034, hostnames are limited to 255 characters.
      if (addrLength > 255) {
      	return false;
      }
      
      // domain labels can contain alphanumerics and '-"
      // but must start and end with an alphanumeric
      char testChar;
      int labelCharCount = 0;

      for (int i = 0; i < addrLength; i++) {
        testChar = address.charAt(i);
        if (testChar == '.') {
          if (!isAlphanum(address.charAt(i-1))) {
            return false;
          }
          if (i+1 < addrLength && !isAlphanum(address.charAt(i+1))) {
            return false;
          }
          labelCharCount = 0;
        }
        else if (!isAlphanum(testChar) && testChar != '-') {
          return false;
        }
        // RFC 1034: Labels must be 63 characters or less.
        else if (++labelCharCount > 63) {
          return false;
        }
      }
    }
    return true;
  }
  
  /**
   * <p>Determines whether a string is an IPv4 address as defined by 
   * RFC 2373, and under the further constraint that it must be a 32-bit
   * address. Though not expressed in the grammar, in order to satisfy 
   * the 32-bit address constraint, each segment of the address cannot 
   * be greater than 255 (8 bits of information).</p>
   *
   * <p><code>IPv4address = 1*3DIGIT "." 1*3DIGIT "." 1*3DIGIT "." 1*3DIGIT</code></p>
   *
   * @return true if the string is a syntactically valid IPv4 address
   */
  private static boolean isWellFormedIPv4Address(String address) {
      
      int addrLength = address.length();
      char testChar;
      int numDots = 0;
      int numDigits = 0;

      // make sure that 1) we see only digits and dot separators, 2) that
      // any dot separator is preceded and followed by a digit and
      // 3) that we find 3 dots
      //
      // RFC 2732 amended RFC 2396 by replacing the definition 
      // of IPv4address with the one defined by RFC 2373. - mrglavas
      //
      // IPv4address = 1*3DIGIT "." 1*3DIGIT "." 1*3DIGIT "." 1*3DIGIT
      //
      // One to three digits must be in each segment.
      for (int i = 0; i < addrLength; i++) {
        testChar = address.charAt(i);
        if (testChar == '.') {
          if ((i > 0 && !isDigit(address.charAt(i-1))) || 
              (i+1 < addrLength && !isDigit(address.charAt(i+1)))) {
            return false;
          }
          numDigits = 0;
          if (++numDots > 3) {
            return false;
          }
        }
        else if (!isDigit(testChar)) {
          return false;
        }
        // Check that that there are no more than three digits
        // in this segment.
        else if (++numDigits > 3) {
          return false;
        }
        // Check that this segment is not greater than 255.
        else if (numDigits == 3) {
          char first = address.charAt(i-2);
          char second = address.charAt(i-1);
          if (!(first < '2' || 
               (first == '2' && 
               (second < '5' || 
               (second == '5' && testChar <= '5'))))) {
            return false;
          }
        }
      }
      return (numDots == 3);
  }
  
  /**
   * <p>Determines whether a string is an IPv6 reference as defined
   * by RFC 2732, where IPv6address is defined in RFC 2373. The 
   * IPv6 address is parsed according to Section 2.2 of RFC 2373,
   * with the additional constraint that the address be composed of
   * 128 bits of information.</p>
   *
   * <p><code>IPv6reference = "[" IPv6address "]"</code></p>
   *
   * <p>Note: The BNF expressed in RFC 2373 Appendix B does not 
   * accurately describe section 2.2, and was in fact removed from
   * RFC 3513, the successor of RFC 2373.</p>
   *
   * @return true if the string is a syntactically valid IPv6 reference
   */
  private static boolean isWellFormedIPv6Reference(String address) {

      int addrLength = address.length();
      int index = 1;
      int end = addrLength-1;
      
      // Check if string is a potential match for IPv6reference.
      if (!(addrLength > 2 && address.charAt(0) == '[' 
          && address.charAt(end) == ']')) {
          return false;
      }
      
      // Counter for the number of 16-bit sections read in the address.
      int [] counter = new int[1];
      
      // Scan hex sequence before possible '::' or IPv4 address.
      index = scanHexSequence(address, index, end, counter);
      if (index == -1) {
          return false;
      }
      // Address must contain 128-bits of information.
      else if (index == end) {
          return (counter[0] == 8);
      }
      
      if (index+1 < end && address.charAt(index) == ':') {
          if (address.charAt(index+1) == ':') {
              // '::' represents at least one 16-bit group of zeros.
              if (++counter[0] > 8) {
                  return false;
              }
              index += 2;
              // Trailing zeros will fill out the rest of the address.
              if (index == end) {
                 return true;
              }
          }
          // If the second character wasn't ':', in order to be valid,
          // the remainder of the string must match IPv4Address, 
          // and we must have read exactly 6 16-bit groups.
          else {
              return (counter[0] == 6) && 
                  isWellFormedIPv4Address(address.substring(index+1, end));
          }
      }
      else {
          return false;
      }
      
      // 3. Scan hex sequence after '::'.
      int prevCount = counter[0];
      index = scanHexSequence(address, index, end, counter);

      // We've either reached the end of the string, the address ends in
      // an IPv4 address, or it is invalid. scanHexSequence has already 
      // made sure that we have the right number of bits. 
      return (index == end) || 
          (index != -1 && isWellFormedIPv4Address(
          address.substring((counter[0] > prevCount) ? index+1 : index, end)));
  }
  
  /**
   * Helper method for isWellFormedIPv6Reference which scans the 
   * hex sequences of an IPv6 address. It returns the index of the 
   * next character to scan in the address, or -1 if the string 
   * cannot match a valid IPv6 address. 
   *
   * @param address the string to be scanned
   * @param index the beginning index (inclusive)
   * @param end the ending index (exclusive)
   * @param counter a counter for the number of 16-bit sections read
   * in the address
   *
   * @return the index of the next character to scan, or -1 if the
   * string cannot match a valid IPv6 address
   */
  private static int scanHexSequence (String address, int index, int end, int [] counter) {
  	
      char testChar;
      int numDigits = 0;
      int start = index;
      
      // Trying to match the following productions:
      // hexseq = hex4 *( ":" hex4)
      // hex4   = 1*4HEXDIG
      for (; index < end; ++index) {
      	testChar = address.charAt(index);
      	if (testChar == ':') {
      	    // IPv6 addresses are 128-bit, so there can be at most eight sections.
      	    if (numDigits > 0 && ++counter[0] > 8) {
      	        return -1;
      	    }
      	    // This could be '::'.
      	    if (numDigits == 0 || ((index+1 < end) && address.charAt(index+1) == ':')) {
      	        return index;
      	    }
      	    numDigits = 0;
        }
        // This might be invalid or an IPv4address. If it's potentially an IPv4address,
        // backup to just after the last valid character that matches hexseq.
        else if (!isHex(testChar)) {
            if (testChar == '.' && numDigits < 4 && numDigits > 0 && counter[0] <= 6) {
                int back = index - numDigits - 1;
                return (back >= start) ? back : (back+1);
            }
            return -1;
        }
        // There can be at most 4 hex digits per group.
        else if (++numDigits > 4) {
            return -1;
        }
      }
      return (numDigits > 0 && ++counter[0] <= 8) ? end : -1;
  } 


 /**
  * Determine whether a char is a digit.
  *
  * @return true if the char is betweeen '0' and '9', false otherwise
  */
  private static boolean isDigit(char p_char) {
    return p_char >= '0' && p_char <= '9';
  }

 /**
  * Determine whether a character is a hexadecimal character.
  *
  * @return true if the char is betweeen '0' and '9', 'a' and 'f'
  *         or 'A' and 'F', false otherwise
  */
  private static boolean isHex(char p_char) {
    return (p_char <= 'f' && (fgLookupTable[p_char] & ASCII_HEX_CHARACTERS) != 0);
  }

 /**
  * Determine whether a char is an alphabetic character: a-z or A-Z
  *
  * @return true if the char is alphabetic, false otherwise
  */
  private static boolean isAlpha(char p_char) {
      return ((p_char >= 'a' && p_char <= 'z') || (p_char >= 'A' && p_char <= 'Z' ));
  }

 /**
  * Determine whether a char is an alphanumeric: 0-9, a-z or A-Z
  *
  * @return true if the char is alphanumeric, false otherwise
  */
  private static boolean isAlphanum(char p_char) {
     return (p_char <= 'z' && (fgLookupTable[p_char] & MASK_ALPHA_NUMERIC) != 0);
  }

 /**
  * Determine whether a character is a reserved character:
  * ';', '/', '?', ':', '@', '&', '=', '+', '$', ',', '[', or ']'
  *
  * @return true if the string contains any reserved characters
  */
  private static boolean isReservedCharacter(char p_char) {
     return (p_char <= ']' && (fgLookupTable[p_char] & RESERVED_CHARACTERS) != 0);
  }

 /**
  * Determine whether a char is an unreserved character.
  *
  * @return true if the char is unreserved, false otherwise
  */
  private static boolean isUnreservedCharacter(char p_char) {
     return (p_char <= '~' && (fgLookupTable[p_char] & MASK_UNRESERVED_MASK) != 0);
  }

 /**
  * Determine whether a char is a URI character (reserved or 
  * unreserved, not including '%' for escaped octets).
  *
  * @return true if the char is a URI character, false otherwise
  */
  private static boolean isURICharacter (char p_char) {
      return (p_char <= '~' && (fgLookupTable[p_char] & MASK_URI_CHARACTER) != 0);
  }

 /**
  * Determine whether a char is a scheme character.
  *
  * @return true if the char is a scheme character, false otherwise
  */
  private static boolean isSchemeCharacter (char p_char) {
      return (p_char <= 'z' && (fgLookupTable[p_char] & MASK_SCHEME_CHARACTER) != 0);
  }

 /**
  * Determine whether a char is a userinfo character.
  *
  * @return true if the char is a userinfo character, false otherwise
  */
  private static boolean isUserinfoCharacter (char p_char) {
      return (p_char <= 'z' && (fgLookupTable[p_char] & MASK_USERINFO_CHARACTER) != 0);
  }
  
 /**
  * Determine whether a char is a path character.
  * 
  * @return true if the char is a path character, false otherwise
  */
  private static boolean isPathCharacter (char p_char) {
      return (p_char <= '~' && (fgLookupTable[p_char] & MASK_PATH_CHARACTER) != 0);
  }


 /**
  * Determine whether a given string contains only URI characters (also
  * called "uric" in RFC 2396). uric consist of all reserved
  * characters, unreserved characters and escaped characters.
  *
  * @return true if the string is comprised of uric, false otherwise
  */
  private static boolean isURIString(String p_uric) {
    if (p_uric == null) {
      return false;
    }
    int end = p_uric.length();
    char testChar = '\0';
    for (int i = 0; i < end; i++) {
      testChar = p_uric.charAt(i);
      if (testChar == '%') {
        if (i+2 >= end ||
            !isHex(p_uric.charAt(i+1)) ||
            !isHex(p_uric.charAt(i+2))) {
          return false;
        }
        else {
          i += 2;
          continue;
        }
      }
      if (isURICharacter(testChar)) {
          continue;
      }
      else {
        return false;
      }
    }
    return true;
  }
}

// This class is taken from the Apache Xerces2 project, with minimal change (renaming, removal of unused code,
// addition of a couple of convenience methods). Previous versions of Saxon-CE used a version of the OpenJDK
// URI class; this has been dropped in order to simplify licensing.

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */