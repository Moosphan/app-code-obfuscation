package com.dorck.app.code.guard.transform

import com.android.build.api.transform.Transform
import com.android.build.api.transform.*
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import com.dorck.app.code.guard.utils.IOUtils
import com.dorck.app.code.guard.utils.DLogger
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

/**
 * Provide base capabilities for processing code from [JarInput] and [DirectoryInput].
 * @author Dorck
 * @since 2023/11/23
 */
abstract class BaseTransform : Transform() {
    override fun transform(transformInvocation: TransformInvocation) {
        onTransformBefore(transformInvocation)
        super.transform(transformInvocation)
        realTransform(transformInvocation)
        onTransformAfter()
    }

    // Config default input types.
    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    // Config default scopes.
    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    protected abstract fun realTransform(transformInvocation: TransformInvocation)

    protected open fun onTransformBefore(transformInvocation: TransformInvocation) {

    }

    protected open fun onTransformAfter() {

    }

    /**
     * 对外暴露的口子，用于处理拦截的情况(如果返回false，则不处理该字节码，默认返回true)
     * 常见场景: class白名单、设定处理的包名范围等
     */
    protected open fun isNeedProcessClass(clzPath: String): Boolean {
        return clzPath.isNotEmpty() && clzPath.endsWith(".class")
    }

    /**
     * Create a class visitor to process bytecode.
     */
    protected abstract fun createClassVisitor(api: Int, delegateClassVisitor: ClassVisitor): ClassVisitor

    // 1.创建output文件夹
    // 2.处理增量更新
    // 3.根据输入文件树生成输出文件树
    // 4.更改输入侧.class字节码并保存
    // 5.将输入侧文件树内容复制到输出侧
    protected fun collectAndHandleDirectories(
        dirInput: DirectoryInput,
        outputProvider: TransformOutputProvider,
        incremental: Boolean
    ) {
        val inputDir = dirInput.file
        val outputDir = outputProvider.getContentLocation(dirInput.name, dirInput.contentTypes, dirInput.scopes, Format.DIRECTORY)
        if (incremental) {
            dirInput.changedFiles.forEach { (file, status) ->
                val changedInputFilePath = file.absolutePath
                val changedOutputFile = File(
                    changedInputFilePath.replace(inputDir.absolutePath, outputDir.absolutePath))
                if (status == Status.ADDED || status == Status.CHANGED) {
                    // Let changed files be transformed with our injected methods.
                    transformClassFile(file, changedOutputFile)
                } else if (status == Status.REMOVED) {
                    changedOutputFile.delete()
                }
            }
        } else {
            handleDirectoriesTransform(inputDir, outputDir)
        }
    }

    /**
     * 递归预创建output文件树，然后寻找.class格式文件并执行代码插桩后复制到输出文件
     */
    private fun handleDirectoriesTransform(inputDir: File, outputDir: File) {
        if (!inputDir.isDirectory) {
            return
        }
        val childrenFiles = inputDir.listFiles()
        childrenFiles?.forEach {
            if (it.isFile) {
                val realOutputFile = File(it.absolutePath.replace(inputDir.absolutePath, outputDir.absolutePath))
                if (!realOutputFile.exists()) {
                    realOutputFile.parentFile.mkdirs()
                }
                realOutputFile.createNewFile()
                transformClassFile(it, realOutputFile)
            } else {
                // 继续递归找到 class 文件
                handleDirectoriesTransform(it, outputDir)
            }
        }
    }

    /**
     * 基于ASM执行具体的代码插桩操作
     */
    private fun transformClassFile(src: File, dest: File) {
        if (src.isDirectory) {
            DLogger.error("transformClassFile, src file is directory!")
            return
        }
        val inputStream = FileInputStream(src)
        val outputStream = FileOutputStream(dest)
        try {
            // 是否需要处理，如针对特定包名做修改，排除白名单中的类
            if (isNeedProcessClass(src.absolutePath)) {
                // 字节码插桩
                val classReader = ClassReader(inputStream)
                val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
                val classVisitor = createClassVisitor(Opcodes.ASM9, classWriter)
                classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
                // 将修改的class文件写到output文件
                outputStream.write(classWriter.toByteArray())
                inputStream.close()
                outputStream.close()
            } else {
                outputStream.write(IOUtils.toByteArray(inputStream)!!)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            inputStream.close()
            outputStream.close()
        }
    }

    protected fun collectAndHandleJars(
        input: JarInput,
        outputProvider: TransformOutputProvider,
        incremental: Boolean
    ) {
        val jarInput = input.file
        val jarOutput = outputProvider.getContentLocation(input.name, input.contentTypes, input.scopes, Format.JAR)
        if (incremental) {
            if (input.status == Status.ADDED || input.status == Status.CHANGED) {
                handleJarsTransform(jarInput, jarOutput)
            } else if (input.status == Status.REMOVED) {
                outputProvider.deleteAll()
            }
        } else {
            handleJarsTransform(jarInput, jarOutput)
        }
    }

    /**
     * 1.将jarInput内容全部复制到jarOutput
     * 2.遇到class文件则需要先执行ASM操作后再复制过去
     */
    protected fun handleJarsTransform(jarInput: File, jarOutput: File) {
        JarFile(jarInput).use { srcJarFile ->
            JarOutputStream(FileOutputStream(jarOutput)).use { destJarFileOs ->
                val enumeration: Enumeration<JarEntry> = srcJarFile.entries()
                //遍历srcJar中的每一条目
                while (enumeration.hasMoreElements()) {
                    val entry = enumeration.nextElement()
                    srcJarFile.getInputStream(entry).use { entryIs ->
                        destJarFileOs.putNextEntry(JarEntry(entry.name))
                        // KLogger.info("Jar path: ${jarOutput.absolutePath}#${entry.name}")
                        if (isNeedProcessClass(entry.name)) { //如果是class文件
                            // 通过asm修改class文件并写入output
                            val classReader = ClassReader(entryIs)
                            val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
                            val classVisitor = createClassVisitor(Opcodes.ASM9, classWriter)
                            classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
                            destJarFileOs.write(classWriter.toByteArray())
                        } else {
                            // 非class原样复制到destJar中
                            destJarFileOs.write(IOUtils.toByteArray(entryIs)!!)
                        }
                        destJarFileOs.closeEntry()
                    }
                }
            }
        }
    }

    private fun log(message: String) {
        DLogger.info("[$TAG] $message")
    }

    companion object {
        private const val TAG = "BaseTransform"
    }
}