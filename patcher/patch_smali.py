"""Applica gli hook RadioFix allo smali del servizio radio EH32 EU. Ogni sostituzione deve
trovare esattamente il numero di occorrenze atteso, altrimenti lo script si ferma."""
import sys, os, re

ROOT = sys.argv[1]  # .../svcmod
R = os.path.join(ROOT, 'smali/com/saicmotor/service/radio')
FIX = 'Lcom/saicmotor/service/radio/fix/RadioFix;'

def patch(rel, old, new, count=1, regex=False):
    p = os.path.join(R, rel)
    s = open(p, encoding='utf-8').read()
    n = len(re.findall(old, s)) if regex else s.count(old)
    if n != count:
        sys.exit('ERRORE %s: trovate %d occorrenze (attese %d) di:\n%s' % (rel, n, count, old))
    s = re.sub(old, new, s) if regex else s.replace(old, new)
    open(p, 'w', encoding='utf-8').write(s)
    print('ok  %-45s x%d' % (rel, count))

# 1) FM: raccogli stazioni solo durante la scansione
patch('RadioService$6.smali',
      '.method public onProgramInfoChanged(Landroid/hardware/radio/RadioManager$ProgramInfo;)V\n'
      '    .locals 12\n'
      '    .param p1, "programInfo"    # Landroid/hardware/radio/RadioManager$ProgramInfo;\n',
      '.method public onProgramInfoChanged(Landroid/hardware/radio/RadioManager$ProgramInfo;)V\n'
      '    .locals 12\n'
      '    .param p1, "programInfo"    # Landroid/hardware/radio/RadioManager$ProgramInfo;\n\n'
      '    invoke-static {p1}, ' + FIX + '->shouldCollect(Landroid/hardware/radio/RadioManager$ProgramInfo;)Z\n\n'
      '    move-result v0\n\n'
      '    if-nez v0, :radiofix_collect\n\n'
      '    return-void\n\n'
      '    :radiofix_collect\n')
patch('platform/RadioTunerExt.smali',
      '.method public scan(IZ)V\n    .locals 5\n    .param p1, "radioType"    # I\n    .param p2, "forward"    # Z\n',
      '.method public scan(IZ)V\n    .locals 5\n    .param p1, "radioType"    # I\n    .param p2, "forward"    # Z\n\n'
      '    invoke-static {}, ' + FIX + '->onScanStart()V\n')
for sig, locals_ in (('seek(IZ)V', 5), ('step(IZ)V', 3)):
    patch('platform/RadioTunerExt.smali',
          '.method public %s\n    .locals %d\n    .param p1, "radioType"    # I\n    .param p2, "forward"    # Z\n' % (sig, locals_),
          '.method public %s\n    .locals %d\n    .param p1, "radioType"    # I\n    .param p2, "forward"    # Z\n\n'
          '    invoke-static {}, %s->onTune()V\n' % (sig, locals_, FIX))
patch('platform/RadioTunerExt.smali',
      '.method public tune(Landroid/hardware/radio/ProgramSelector;)V\n    .locals 2\n'
      '    .param p1, "selector"    # Landroid/hardware/radio/ProgramSelector;\n'
      '        .annotation build Landroidx/annotation/NonNull;\n        .end annotation\n    .end param\n',
      '.method public tune(Landroid/hardware/radio/ProgramSelector;)V\n    .locals 2\n'
      '    .param p1, "selector"    # Landroid/hardware/radio/ProgramSelector;\n'
      '        .annotation build Landroidx/annotation/NonNull;\n        .end annotation\n    .end param\n\n'
      '    invoke-static {}, ' + FIX + '->onTune()V\n')
patch('RadioService.smali',
      '.method private amOrFmScanOver()V\n    .locals 3\n',
      '.method private amOrFmScanOver()V\n    .locals 3\n\n'
      '    invoke-static {}, ' + FIX + '->onScanOver()V\n')

# 2) DAB loghi: errore "size() - 1" nel ciclo di onDabServiceLogoChanged
patch('RadioService$7.smali',
      '    invoke-interface {v5}, Ljava/util/List;->size()I\n\n'
      '    move-result v5\n\n'
      '    add-int/lit8 v5, v5, -0x1\n\n'
      '    if-ge v3, v5, :cond_2\n',
      '    invoke-interface {v5}, Ljava/util/List;->size()I\n\n'
      '    move-result v5\n\n'
      '    if-ge v3, v5, :cond_2\n')

# 3) DAB loghi: accetta anche loghi rettangolari "logo_unrestricted"
patch('RadioService$7.smali',
      '    invoke-virtual {v4}, Landroid/hardware/radio/RadioDabInfo$LogoInfo;->getLogoType()Ljava/lang/String;\n\n'
      '    move-result-object v5\n\n'
      '    const-string v6, "logo_unrestricted"\n\n'
      '    invoke-virtual {v5, v6}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z\n\n'
      '    move-result v5\n\n'
      '    if-eqz v5, :cond_4\n\n'
      '    .line 1627\n'
      '    invoke-virtual {v4}, Landroid/hardware/radio/RadioDabInfo$LogoInfo;->getHeight()I\n\n'
      '    move-result v5\n\n'
      '    invoke-virtual {v4}, Landroid/hardware/radio/RadioDabInfo$LogoInfo;->getWidth()I\n\n'
      '    move-result v6\n\n'
      '    if-ne v5, v6, :cond_4\n',
      '    invoke-static {v4}, ' + FIX + '->acceptLogo(Landroid/hardware/radio/RadioDabInfo$LogoInfo;)Z\n\n'
      '    move-result v5\n\n'
      '    if-eqz v5, :cond_4\n')

# 4) loghi decodificati sempre quadrati (ritaglio/letterbox)
for rel, n in (('RadioService$7.smali', 2), ('RadioService$5.smali', 1)):
    patch(rel, 'Landroid/graphics/BitmapFactory;->decodeByteArray([BII)Landroid/graphics/Bitmap;',
          FIX + '->decodeLogo([BII)Landroid/graphics/Bitmap;', n)

# 5) liste loghi / info servizi con deduplica e limite
patch('RadioService$7.smali',
      r'invoke-interface \{(v\d+), (v\d+)\}, Ljava/util/List;->add\(Ljava/lang/Object;\)Z',
      r'invoke-static {\1, \2}, ' + FIX.replace('$', r'\$') + r'->addCapped(Ljava/util/List;Ljava/lang/Object;)Z',
      2, regex=True)

# 6) slideshow: limite, home (launcher) e copertina per stazione
patch('RadioService$7.smali',
      '    invoke-virtual {v0, p1}, Lcom/saicmotor/sdk/radio/bean/RadioBean;->addDabSlideShow(Landroid/graphics/Bitmap;)V\n',
      '    iget-object v1, p0, Lcom/saicmotor/service/radio/RadioService$7;->this$0:Lcom/saicmotor/service/radio/RadioService;\n\n'
      '    invoke-static {v1, v0, p1}, ' + FIX + '->onSlide(Lcom/saicmotor/service/radio/RadioService;Lcom/saicmotor/sdk/radio/bean/RadioBean;Landroid/graphics/Bitmap;)V\n')
patch('RadioService$5.smali',
      '.method public onCurrentProgramChange(Lcom/saicmotor/service/radio/storage/entity/DabServiceInfo;)V\n'
      '    .locals 6\n'
      '    .param p1, "dabServiceInfo"    # Lcom/saicmotor/service/radio/storage/entity/DabServiceInfo;\n',
      '.method public onCurrentProgramChange(Lcom/saicmotor/service/radio/storage/entity/DabServiceInfo;)V\n'
      '    .locals 6\n'
      '    .param p1, "dabServiceInfo"    # Lcom/saicmotor/service/radio/storage/entity/DabServiceInfo;\n\n'
      '    iget-object v0, p0, Lcom/saicmotor/service/radio/RadioService$5;->this$0:Lcom/saicmotor/service/radio/RadioService;\n\n'
      '    invoke-static {v0}, ' + FIX + '->onStationChanged(Lcom/saicmotor/service/radio/RadioService;)V\n')

# 7) ripresa automatica all'avvio
patch('audio/AudioController.smali',
      '.method public onAudioFocusChange(I)V\n    .locals 4\n    .param p1, "focusChange"    # I\n',
      '.method public onAudioFocusChange(I)V\n    .locals 4\n    .param p1, "focusChange"    # I\n\n'
      '    invoke-static {p1}, ' + FIX + '->onFocusChange(I)V\n')
patch('RadioService.smali',
      '.method public synthetic lambda$initRadioListener$0$RadioService(Lcom/saicmotor/sdk/radio/bean/RadioBean;)V\n'
      '    .locals 8\n'
      '    .param p1, "programInfo"    # Lcom/saicmotor/sdk/radio/bean/RadioBean;\n',
      '.method public synthetic lambda$initRadioListener$0$RadioService(Lcom/saicmotor/sdk/radio/bean/RadioBean;)V\n'
      '    .locals 8\n'
      '    .param p1, "programInfo"    # Lcom/saicmotor/sdk/radio/bean/RadioBean;\n\n'
      '    invoke-static {p0, p1}, ' + FIX + '->onProgram(Lcom/saicmotor/service/radio/RadioService;Lcom/saicmotor/sdk/radio/bean/RadioBean;)V\n')
patch('options/LastMode.smali',
      '    invoke-virtual {v1}, Lcom/saicmotor/service/radio/storage/RadioStorage;->getRecentlySelectedState()I\n\n'
      '    move-result v1\n',
      '    invoke-virtual {v1}, Lcom/saicmotor/service/radio/storage/RadioStorage;->getRecentlySelectedState()I\n\n'
      '    move-result v1\n\n'
      '    invoke-static {v1}, ' + FIX + '->resumeState(I)I\n\n'
      '    move-result v1\n')
patch('RadioService.smali',
      '    invoke-direct {p0}, Lcom/saicmotor/service/radio/RadioService;->lastModeSource()V\n\n'
      '    .line 191\n'
      '    return-void\n',
      '    invoke-direct {p0}, Lcom/saicmotor/service/radio/RadioService;->lastModeSource()V\n\n'
      '    invoke-static {p0}, ' + FIX + '->scheduleBootResume(Lcom/saicmotor/service/radio/RadioService;)V\n\n'
      '    .line 191\n'
      '    return-void\n')
print('tutti gli hook applicati')
