/**
 * The MIT License (MIT)
 *
 * Copyright (C) 2018 CheatBreaker, LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package SassoObfuscator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import SassoObfuscator.transformer.AccessTransformer;
import SassoObfuscator.transformer.ConstantTransformer;
import SassoObfuscator.transformer.JunkFieldTransformer;
import SassoObfuscator.transformer.ShuffleTransformer;
import SassoObfuscator.transformer.StringTransformer;
import SassoObfuscator.transformer.Transformer;
import SassoObfuscator.utils.StreamUtils;

public class Obf {

    private final Random random;
    HashMap<String, String> nameMap = new HashMap<>();
    private final List<ClassNode> classes = new ArrayList<>();
    private final List<Transformer> transformers = new ArrayList<>();
    private final List<ClassNode> newClasses = new ArrayList<>();

    public Obf(File inputFile, File outputFile) throws IOException {
        random = new Random();

        transformers.add(new ConstantTransformer(this));
        transformers.add(new StringTransformer(this));
        transformers.add(new JunkFieldTransformer(this));
        transformers.add(new AccessTransformer(this));
        transformers.add(new ShuffleTransformer(this));

        JarFile inputJar = new JarFile(inputFile);

        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(outputFile))) {

            // read all classes into this.classes and copy all resources to output jar
            System.out.println("Prendendo il jar...");
            for (Enumeration<JarEntry> iter = inputJar.entries(); iter.hasMoreElements(); ) {
                JarEntry entry = iter.nextElement();
                try (InputStream in = inputJar.getInputStream(entry)) {
                    if (entry.getName().endsWith(".class")) {
                        ClassReader reader = new ClassReader(in);
                        ClassNode classNode = new ClassNode();
                        reader.accept(classNode, 0);
                        classes.add(classNode);
                    } else {
                    	
                        String nomeclasse = entry.getName();
                        if(nomeclasse.equals("Main") || nomeclasse.equals("Minecraft") || nomeclasse.equals("Start")) {
                        	return;
                        }
             
                 
                        out.putNextEntry(new JarEntry(entry.getName()));
                        StreamUtils.copy(in, out);
                    }
                }
            }

            // shuffle the entries in case the order in the output jar gives away information
            Collections.shuffle(classes, random);

            System.out.println("Inizio Trasformazione classi...");
            for (Transformer transformer : transformers) {
                System.out.println("Sto Caricando " + transformer.getClass().getSimpleName() + "...");
                classes.forEach(transformer :: visit);
            }
            for (Transformer transformer : transformers) {
                transformer.after();
            }

            System.out.println("Copiando Le Classi Nel Nuovo Jar...");
            for (ClassNode classNode : classes) {
                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                classNode.accept(writer);
                String nomeclasse = classNode.name.toString();
                	  out.putNextEntry(new JarEntry(nomeclasse + ".class"));
                out.write(writer.toByteArray());
            }

            System.out.println("Generando le nuove Classi...");
            for (ClassNode classNode : newClasses) {
                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
                classNode.accept(writer);
                out.putNextEntry(new JarEntry(classNode.name + ".class"));
                out.write(writer.toByteArray());
            }
        }
    }

    public Random getRandom() {
        return random;
    }

    public List<ClassNode> getClasses() {
        return classes;
    }

    public void addNewClass(ClassNode classNode) {
        newClasses.add(classNode);
    }
}
