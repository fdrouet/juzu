/*
 * Copyright (C) 2011 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.juzu.impl.compiler;

import org.juzu.impl.spi.fs.FileSystem;

import javax.annotation.processing.Processor;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class CompilerContext<P, D extends P, F extends P>
{

   /** . */
   final FileSystem<P, D, F> fs;

   /** . */
   private JavaCompiler compiler;

   /** . */
   private VirtualFileManager fileManager;

   /** . */
   private Set<Processor> processors;

   public CompilerContext(FileSystem<P, D, F> fs)
   {
      this.fs = fs;
      this.compiler = ToolProvider.getSystemJavaCompiler();
      this.fileManager = new VirtualFileManager(compiler.getStandardFileManager(null, null, null));
      this.processors = new HashSet<Processor>();
   }

   public void addAnnotationProcessor(Processor annotationProcessorType)
   {
      if (annotationProcessorType == null)
      {
         throw new NullPointerException("No null processor allowed");
      }
      processors.add(annotationProcessorType);
   }

   public Map<String, ClassFile> compile() throws IOException
   {
      Collection<VirtualJavaFileObject.FileSystem<P, F>> sources = collectJavaFiles();

      // Filter compiled files
      for (Iterator<VirtualJavaFileObject.FileSystem<P, F>> i = sources.iterator();i.hasNext();)
      {
         VirtualJavaFileObject.FileSystem<P, F> source = i.next();
         FileKey key = source.key;
         VirtualJavaFileObject.CompiledClass existing = (VirtualJavaFileObject.CompiledClass)fileManager.files.get(new FileKey(key.rawPath, JavaFileObject.Kind.CLASS));
         if (existing != null)
         {
            ClassFile cf = existing.getFile();
            if (cf != null && cf.getLastModified() >= source.getLastModified())
            {
               i.remove();
            }
         }
      }

      //
      JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, null, null, sources);
      task.setProcessors(processors);

      //
      if (task.call())
      {
         Map<String, ClassFile> files = new HashMap<String, ClassFile>();
         for (VirtualJavaFileObject.CompiledClass clazz : fileManager.modifications)
         {
            files.put(clazz.className, clazz.getFile());
         }
         fileManager.modifications.clear();
         return files;
      }
      else
      {
         return null;
      }
   }

   public Collection<VirtualJavaFileObject.FileSystem<P, F>> collectJavaFiles() throws IOException
   {
      D root = fs.getRoot();
      ArrayList<VirtualJavaFileObject.FileSystem<P, F>> javaFiles = new ArrayList<VirtualJavaFileObject.FileSystem<P, F>>();
      collectJavaFiles(root, javaFiles);
      return javaFiles;
   }

   private void collectJavaFiles(D dir, ArrayList<VirtualJavaFileObject.FileSystem<P, F>> javaFiles) throws IOException
   {
      for (Iterator<P> i = fs.getChildren(dir);i.hasNext();)
      {
         P child = i.next();
         if (fs.isFile(child))
         {
            String name = fs.getName(child);
            if (name.endsWith(".java"))
            {
               F javaFile = fs.asFile(child);
               FileKey key = getURI(javaFile);
               javaFiles.add(new VirtualJavaFileObject.FileSystem<P, F>(this, javaFile, key));
            }
         }
         else
         {
            D childDir = fs.asDir(child);
            collectJavaFiles(childDir, javaFiles);
         }
      }
   }

   private StringBuilder foo(P file) throws IOException
   {
      P parent = fs.getParent(file);
      if (parent == null)
      {
         return new StringBuilder("/");
      }
      else if (fs.equals(parent, fs.getRoot()))
      {
         return new StringBuilder("/").append(fs.getName(file));
      }
      else
      {
         return foo(parent).append('/').append(fs.getName(file));
      }
   }

   public FileKey getURI(F file) throws IOException
   {
      String name = fs.getName(file);
      if (!name.endsWith(".java"))
      {
         throw new IllegalArgumentException("File " + name + " is not a java source file");
      }
      String rawName = name.substring(0, name.length() - ".java".length());
      StringBuilder foo = foo(fs.getParent(file));
      if (foo.length() == 1)
      {
         foo.append(rawName);
      }
      else
      {
         foo.append('/').append(rawName);
      }
      return new FileKey(foo.toString(), JavaFileObject.Kind.SOURCE);
   }
}
