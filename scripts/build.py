#!/usr/bin/env python3
"""Dependency-light Linux build. Requires Python3 + Java17 runtime; downloads verified official tools."""
import hashlib, os, pathlib, shutil, subprocess, sys, urllib.request, zipfile
import xml.etree.ElementTree as ET
ROOT=pathlib.Path(__file__).resolve().parents[1]
TOOLS=pathlib.Path(os.environ.get('CLASSNOTE_TOOLS',str(ROOT/'.tools')))
BUILD=ROOT/'build/manual'
def run(*args):
    subprocess.run([str(a) for a in args],check=True,cwd=ROOT)
def get(url,name,sha):
    dest=TOOLS/name
    if not dest.exists() or hashlib.sha256(dest.read_bytes()).hexdigest()!=sha:
        dest.parent.mkdir(parents=True,exist_ok=True)
        with urllib.request.urlopen(url,timeout=120) as source,open(dest,'wb') as out:shutil.copyfileobj(source,out)
    if hashlib.sha256(dest.read_bytes()).hexdigest()!=sha:raise RuntimeError('Checksum failed: '+name)
    return dest
def unpack(archive,dest):
    if not dest.exists():
        with zipfile.ZipFile(archive) as z:
            z.extractall(dest)
            for member in z.infolist():
                mode=member.external_attr>>16
                if mode&0o111:(dest/member.filename).chmod(0o755)
def main():
    if not sys.platform.startswith('linux'):raise SystemExit('Use Android Studio/gradlew.bat on Windows, or run this script in WSL/Linux.')
    ecj=get('https://repo.maven.apache.org/maven2/org/eclipse/jdt/ecj/3.40.0/ecj-3.40.0.jar','ecj.jar','05cc22a24e7982970f63a405fc6c820bc80b806f27f3c5a6236fc475f8f7152b')
    btzip=get('https://dl.google.com/android/repository/build-tools_r35_linux.zip','build-tools.zip','bd3a4966912eb8b30ed0d00b0cda6b6543b949d5ffe00bea54c04c81e1561d88')
    plat=get('https://dl.google.com/android/repository/platform-35_r02.zip','platform.zip','0988cacad01b38a18a47bac14a0695f246bc76c1b06c0eeb8eb0dc825ab0c8e0')
    unpack(btzip,TOOLS/'build-tools');unpack(plat,TOOLS/'platform')
    bt=TOOLS/'build-tools/android-15';android=TOOLS/'platform/android-35/android.jar'
    if BUILD.exists():shutil.rmtree(BUILD)
    (BUILD/'generated').mkdir(parents=True);(BUILD/'classes').mkdir()
    ET.register_namespace('android','http://schemas.android.com/apk/res/android')
    manifest=ET.parse(ROOT/'app/src/main/AndroidManifest.xml')
    manifest.getroot().set('package','cn.cbhcool.classnote')
    manifest.write(BUILD/'AndroidManifest.xml',encoding='utf-8',xml_declaration=True)
    run(bt/'aapt2','compile','--dir',ROOT/'app/src/main/res','-o',BUILD/'resources.zip')
    run(bt/'aapt2','link','-o',BUILD/'base.apk','-I',android,'--manifest',BUILD/'AndroidManifest.xml','--java',BUILD/'generated','--auto-add-overlay','-A',ROOT/'app/src/main/assets',BUILD/'resources.zip')
    javafiles=list((ROOT/'app/src/main/java').rglob('*.java'))+list((BUILD/'generated').rglob('*.java'))
    run('java','-jar',ecj,'-1.8','-encoding','UTF-8','-bootclasspath',str(android)+os.pathsep+str(bt/'core-lambda-stubs.jar'),'-d',BUILD/'classes',*javafiles)
    run(bt/'d8','--min-api','26','--lib',android,'--output',BUILD,*list((BUILD/'classes').rglob('*.class')))
    unsigned=BUILD/'unsigned.apk';shutil.copy2(BUILD/'base.apk',unsigned)
    with zipfile.ZipFile(unsigned,'a',compression=zipfile.ZIP_DEFLATED) as apk:
        for dex in BUILD.glob('*.dex'):apk.write(dex,dex.name)
    run(bt/'zipalign','-f','4',unsigned,BUILD/'aligned.apk')
    key=ROOT/'signing/personal.jks';key.parent.mkdir(exist_ok=True)
    if not key.exists():run('keytool','-genkeypair','-keystore',key,'-storepass','classnote-local','-keypass','classnote-local','-alias','personal','-keyalg','RSA','-keysize','2048','-validity','10000','-dname','CN=ClassNote Personal, O=Personal Development, C=CN')
    apk=BUILD/'classnote-1.5.1.apk'
    run(bt/'apksigner','sign','--ks',key,'--ks-key-alias','personal','--ks-pass','pass:classnote-local','--key-pass','pass:classnote-local','--out',apk,BUILD/'aligned.apk')
    run(bt/'apksigner','verify','--verbose',apk)
    print('APK:',apk)
if __name__=='__main__':main()
