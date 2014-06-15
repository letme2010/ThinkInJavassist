package org.jamruby.javassistsample;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.android.DexFile;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import dalvik.system.DexClassLoader;

public class MainActivity extends Activity {

	private static final String DEX_FILE_NAME_CLASSES = "ltclasses.dex";

	public static class ClassHelper {
		public static Class<?> convert2javaClass(CtClass aCtClass, Context aContext) throws IOException,
				ClassNotFoundException {

			final File dexFile = new File(aContext.getFilesDir(), DEX_FILE_NAME_CLASSES);
			final String dexFilePath = dexFile.getAbsolutePath();

			// convert from "xxx.class" to "xxx.dex"
			final DexFile df = new DexFile();
			df.addClass(new File(aContext.getFilesDir(), aCtClass.getName() + ".class"));
			df.writeFile(dexFilePath);

			if (dexFile.exists()) {
				final DexClassLoader dcl = new DexClassLoader(dexFile.getAbsolutePath(), aContext.getCacheDir()
						.getAbsolutePath(), aContext.getApplicationInfo().nativeLibraryDir, aContext.getClassLoader());
				Class<?> ret = dcl.loadClass(aCtClass.getName());
				return ret;
			}

			return null;

		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		RelativeLayout content = new RelativeLayout(this);
		content.setBackgroundColor(Color.WHITE);
		this.setContentView(content);
		
		final File dexFile = new File(this.getFilesDir(), DEX_FILE_NAME_CLASSES);
		if (dexFile.exists()) {
			dexFile.delete();
		}
		
		try {
			final ClassPool cp = ClassPool.getDefault(getApplicationContext());

			CtClass ctViewClass = cp.get("android.view.View");

			final CtClass ctViewExClass = cp.makeClass("ViewEx", ctViewClass);
			final CtConstructor ctConstructor = new CtConstructor(new CtClass[]{cp.get("android.content.Context")}, ctViewExClass);
			ctConstructor.setBody("{super($1);}");
			ctViewExClass.addConstructor(ctConstructor);

			CtMethod dispatchTouchEventMethod = CtMethod.make("public boolean dispatchTouchEvent("+MotionEvent.class.getName()+" event) {System.out.println(\"LETME before.\");boolean ret = super.dispatchTouchEvent(event);System.out.println(\"LETME after.\");return ret;}", ctViewExClass);
			ctViewExClass.addMethod(dispatchTouchEventMethod);
			
			ctViewExClass.writeFile(getFilesDir().getAbsolutePath());

			
			
			
			
			final Class<?> class_hoge = ClassHelper.convert2javaClass(ctViewExClass, this);

			Constructor<?> constructor = null;

			for (Constructor<?> c : class_hoge.getConstructors()) {
				if (1 == c.getParameterTypes().length) {
					constructor = c;
					break;
				}
			}

			final Object obj = constructor.newInstance(this);

			System.out.println("LETME CLASS:" + obj.getClass());
			System.out.println("LETME instanceof View : " + (obj instanceof View));
			if (obj instanceof View) {
				View view = (View) obj;
				view.dispatchTouchEvent(null);
			}
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
