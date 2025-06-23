<script setup>

import {Check, Document} from "@element-plus/icons-vue";
import {computed, reactive, ref} from "vue";
import {Delta, Quill, QuillEditor} from "@vueup/vue-quill";
import ImageResize from "quill-image-resize-vue";
import { ImageExtend, QuillWatch } from "quill-image-super-solution-module";
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import {ElMessage} from "element-plus";
import {accessHeader, get, post} from "@/net";
import axios from "axios";
import ColorDot from "@/components/ColorDot.vue";
import {useStore} from "@/store";

console.log('ImageResize:', ImageResize);
console.log('ImageExtend:', ImageExtend);
console.log('QuillWatch:', QuillWatch);

const store=useStore()

const props= defineProps({
  show:Boolean,
  defaultTitle:{
      default:'',
      type:String
  },
  defaultText:{
    default:'',
    type:String
  },
  defaultType:{
    default: null,
    type:Number
  },
  submitButton:{
    default:'立即发布帖子',
    type:String
  },
  submit:{
    default:(editor,success)=>{
      post('api/forum/create-topic',{
        type:editor.type.id,
        title: editor.title,
        content: editor.text
      },()=>{
        ElMessage.success("帖子发表成功")
        success()
      })
    },
    type:Function
  }
})

const refEditor=ref()

const emit=defineEmits(['close','success'])

const editor=reactive({
  type:null,
  title:'',
  text:'',
  loading: false
})

function initEditor(){
  if (props.defaultText)
    editor.text=new Delta(JSON.parse(props.defaultText))
  else
     refEditor.value.setContents('', 'user')
  editor.title=props.defaultTitle
  editor.type=findTypeById(props.defaultType)
}


function deltaToText(delta) {
  if(!delta.ops) return ""
  let str = ""
  for (let op of delta.ops)
    str += op.insert
  return str.replace(/\s/g, "")
}

const contentLength = computed(() => deltaToText(editor.text).length)

function findTypeById(id){
  for (let type of store.forum.types) {
    if (type.id===id)
      return type
  }
}

function submitTopic(){
  const text=deltaToText(editor.text)
  console.log('Text length:', text.length);
  if (text.length>20000){
    console.log("字数超过所以被打印了，字数为:"+text.length)
    ElMessage.warning("字数超出限制,无法发表")
    return
  }
  if(!editor.title){
    ElMessage.warning("请填写标题")
    return
  }
  if (!editor.type){
    ElMessage.warning("请选择合适的帖子类型")
    return
  }
  props.submit(editor,()=>emit('success'))
}



Quill.register('modules/ImageExtend', ImageExtend);
Quill.register('modules/imageResize', ImageResize);
const editorOption = {
  modules: {
    toolbar: {
      container: [
        "bold", "italic", "underline", "strike","clean",
        {color: []}, {'background': []},
        {size: ["small", false, "large", "huge"]},
        { header: [1, 2, 3, 4, 5, 6, false] },
        {list: "ordered"}, {list: "bullet"}, {align: []},
        "blockquote", "code-block", "link", "image",
        { indent: '-1' }, { indent: '+1' }
      ],
      handlers: {
        'image': function () {
          QuillWatch.emit(this.quill.id)
        }
      }
    },
    imageResize: {
      modules: [ 'Resize', 'DisplaySize' ]
    },
    ImageExtend: {
      action:  axios.defaults.baseURL + '/api/image/cache',
      name: 'file',
      size: 5,
      loading: true,
      accept: 'image/png, image/jpeg',
      response: (resp) => {
        if(resp.data) {
          return axios.defaults.baseURL + '/images' + resp.data
        } else {
          return null
        }
      },
      methods: 'POST',
      headers: xhr => {
        xhr.setRequestHeader('Authorization', accessHeader().Authorization);
      },
      start: () => editor.uploading = true,
      success: () => {
        ElMessage.success('图片上传成功!')
        editor.uploading = false
      },
      error: () => {
        ElMessage.warning('图片上传失败，请联系管理员!')
        editor.uploading = false
      }
    }
  }
}

</script>

<template>
    <el-drawer :model-value="show" direction="btt"
               :size="650"
               @open="initEditor"
               @close="emit('close')"
                :close-on-click-modal="false">
      <template #header>
        <div>
          <div style="font-weight: bold">发表新的帖子</div>
          <div style="font-size: 13px">发表内容之前，请遵守网站规定</div>
        </div>
      </template>
      <div style="display: flex;gap: 10px">
        <div style="width: 150px">
          <el-select  v-model="editor.type" value-key="id" placeholder="请选择帖子类型..." :disabled="!store.forum.types.length">
            <el-option v-for="item in store.forum.types.filter(type=>type.id>0)" :value="item" :label="item.name">
              <div>
                <color-dot :color="item.color"/>
                <span style="margin-left: 10px">{{item.name}}</span>
              </div>
            </el-option>
          </el-select>
        </div>
        <div style="flex: 1" >
          <el-input   maxlength="30"  v-model="editor.title" style="height: 100%"
              placeholder="请输入帖子标题..." :prefix-icon="Document"/>
        </div>
      </div>
      <div style="margin-top: 5px;font-size: 13px;color: grey">
        <color-dot :color="editor.type ? editor.type.color : '#959191' "/>
       <span style="margin-left: 5px">{{editor.type ?editor.type.desc:'请在上方选择一个帖子类型'}}</span>
      </div>
      <div style="margin-top: 10px;height: 440px;overflow: hidden;border-radius: 5px"
           v-loading="editor.uploading"
           element-loading-text="这种上传图片，请稍后...">
        <quill-editor v-model:content="editor.text" style="height: calc(100% - 45px)"
                      content-type="delta" ref="refEditor"
                      placeholder="今天想分享点什么呢？" :options="editorOption"/>
      </div>
      <div style="display: flex;justify-content: space-between;margin-top: 5px">
          <div style="color: grey;font-size: 13px">
            当前字数{{contentLength}}(最大字数20000字)
          </div>
          <div>
            <el-button type="success"  @click="submitTopic" :icon="Check" plain>{{submitButton}}</el-button>
          </div>

      </div>
    </el-drawer>
</template>

<style lang="less" scoped>
:deep(.el-drawer){
  width: 800px;
  margin: auto;
  border-radius: 10px 10px 0 0;
}
:deep(.el-drawer__header){
  margin: 0;
}


</style>