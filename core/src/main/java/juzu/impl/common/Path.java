/*
 * Copyright 2013 eXo Platform SAS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package juzu.impl.common;

import java.io.Serializable;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public abstract class Path implements Serializable {

  /** . */
  private static final String[] EMPTY_STRING_ARRAY = new String[0];

  public static Absolute absolute(Name name, String ext) {
    return new Absolute(name, ext);
  }

  public static Absolute absolute(Name qn, String rawName, String ext) {
    return new Absolute(qn, rawName, ext);
  }

  public static Relative relative(Name qn, String rawName, String ext) {
    return new Relative(qn, rawName, ext);
  }

  public static Relative relative(Name name, String ext) {
    return new Relative(name, ext);
  }

  public static Path create(boolean absolute, Name qn, String rawName, String ext) {
    if (absolute) {
      return absolute(qn, rawName,  ext);
    } else {
      return relative(qn, rawName, ext);
    }
  }

  public static Path parse(String path) throws NullPointerException, IllegalArgumentException {
    boolean absolute = path.length() > 0 && path.charAt(0) == '/';
    String[] atoms = Lexers.parsePath(Lexers.PARSE_CANONICAL, EMPTY_STRING_ARRAY, 0, path, 0);
    if (absolute) {
      return new Absolute(new Name(atoms, atoms.length - 1), atoms[atoms.length - 1]);
    } else {
      return new Relative(new Name(atoms, atoms.length - 1), atoms[atoms.length - 1]);
    }
  }

  /** . */
  protected final Name name;

  /** . */
  protected final String ext;

  /** . */
  private String canonical;

  private Path(Name pkg, String rawName, String ext) {
    this.canonical = null;
    this.name = pkg.append(rawName);
    this.ext = ext;
  }

  private Path(Name name, String ext) {
    this.canonical = null;
    this.name = name;
    this.ext = ext;
  }

  public Path append(String path) throws NullPointerException, IllegalArgumentException {
    if (path == null) {
      throw new NullPointerException("No null path accepted");
    }
    if (path.length() > 0 && path.charAt(0) == '/') {
      throw new IllegalArgumentException("Cannot append absolute path " + path);
    }
    String[] atoms = Lexers.parsePath(Lexers.PARSE_ANY, name.identifiers, name.size - 1, path, 0);
    return create(new Name(atoms, atoms.length - 1), atoms[atoms.length - 1]);
  }

  public String getValue() {
    return getCanonical();
  }

  /**
   * Covariant create.
   *
   * @param name the name
   * @param ext the extension
   * @return the path
   */
  protected abstract Path create(Name name, String ext);

  public final boolean isAbsolute() {
    return this instanceof Absolute;
  }

  public final boolean isRelative() {
    return !isAbsolute();
  }

  /**
   * Returns the path directories as a name.
   *
   * @return the dirs
   */
  public Name getDirs() {
    return name.getParent();
  }

  /**
   * Returns the path as a name.
   *
   * @return the name
   */
  public Name getName() {
    return name;
  }

  /**
   * Returns the path raw name.
   *
   * @return the raw name
   */
  public String getRawName() {
    return name.getIdentifier();
  }

  /**
   * Returns the path extension.
   *
   * @return the extension
   */
  public String getExt() {
    return ext;
  }

  /**
   * Returns the path simple name: the raw name followed by the extension.
   *
   * @return the simple name
   */
  public String getSimpleName() {
    String ext = getExt();
    String rawName = getRawName();
    if (ext != null) {
      return rawName + "." + ext;
    }
    else {
      return rawName;
    }
  }

  public Path as(String ext) {
    return create(name, ext);
  }

  public Path as(String rawName, String ext) {
    String[] tmp = new String[name.size];
    System.arraycopy(name.identifiers, 0, tmp, 0, name.size - 1);
    tmp[tmp.length - 1] = rawName;
    return create(new Name(tmp, tmp.length), ext);
  }

  public String getCanonical() {
    if (canonical == null) {
      StringBuilder sb = new StringBuilder();
      if (isAbsolute()) {
        sb.append('/');
      }
      for (int i = 0;i < name.size();i++) {
        if (i > 0) {
          sb.append('/');
        }
        sb.append(name.get(i));
      }
      String ext = getExt();
      if (ext != null) {
        sb.append('.').append(ext);
      }
      canonical = sb.toString();
    }
    return canonical;
  }

  @Override
  public int hashCode() {
    // Should use ext too in hashcode
    return name.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj.getClass() == getClass()) {
      Path that = (Path)obj;
      return isAbsolute() == that.isAbsolute() && name.equals(that.name) && Tools.safeEquals(ext, that.ext);
    }
    return false;
  }

  @Override
  public String toString() {
    return "Path[" + getCanonical() +  "]";
  }

  public static class Absolute extends Path {

    public Absolute(Name pkg, String rawName, String ext) {
      super(pkg, rawName, ext);
    }

    public Absolute(Name name, String ext) {
      super(name, ext);
    }

    @Override
    protected Absolute create(Name name, String ext) {
      return new Absolute(name, ext);
    }

    @Override
    public Absolute as(String ext) {
      return (Absolute)super.as(ext);
    }

    @Override
    public Absolute as(String rawName, String ext) {
      return (Absolute)super.as(rawName, ext);
    }
  }

  public static class Relative extends Path {

    public Relative(Name pkg, String rawName, String ext) {
      super(pkg, rawName, ext);
    }

    public Relative(Name name, String ext) {
      super(name, ext);
    }

    @Override
    protected Relative create(Name name, String ext) {
      return new Relative(name, ext);
    }

    @Override
    public Relative as(String ext) {
      return (Relative)super.as(ext);
    }

    @Override
    public Relative as(String rawName, String ext) {
      return (Relative)super.as(rawName, ext);
    }
  }
}
