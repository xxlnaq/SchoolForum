<script setup>

import Card from "@/components/Card.vue";
import {Setting, Switch} from "@element-plus/icons-vue";
import {reactive, ref} from "vue";
import {get, post} from "@/net";
import {ElMessage} from "element-plus";

const from=reactive({
  password:'',
  new_password:'',
  new_password_repeat:''
})

const validatePassword=(rule,value,callback)=>{
  if (value===''){
    callback(new Error("请再次输入密码"))
  }
   else if (value!==from.new_password){
    callback(new  Error("两次输入的密码不一致"))
  }
   else {
     callback()
  }
}

const rules={
  password:[
    {required:true,message:'请输入密码',trigger:'blur'}
  ],
  new_password:[
    {required:true,message:'请输入新的密码',trigger:'blur'},
    {min:6,max:20,message:'密码的长度为6-16之间',trigger: 'blur'}
  ],
  new_password_repeat:[
    {required:true,message:'请再次输入新的密码',trigger:'blur'},
    {validator:validatePassword,trigger: ['blur','change']}
  ]
}

const formRef=ref()
const valid=ref(false)
const onValidate=(prop,isValid)=>valid.value=isValid

function  resetPassword(){
  formRef.value.validate(valid=>{
    if (valid){
    post('api/user/change-password' ,from ,()=>{
      ElMessage.success("修改密码成功")
      formRef.value.resetFields();
    },(message)=>{
      ElMessage.error(message)
    })
    }
  })
}

const  saving=ref(true)

const  privacy=reactive({
  phone:false,
  wx:false,
  qq:false,
  email:false,
  gender:false
})

get('api/user/privacy',data=>{
  privacy.email=data.email
  privacy.gender=data.gender
  privacy.qq=data.qq
  privacy.wx=data.wx
  privacy.phone=data.phone
  saving.value=false
})

function  savePrivacy(type,status){
  saving.value=true
  post('/api/user/save-privacy',{
    type:type,
    status:status
  },()=>{
    ElMessage.success('隐私设置修改成功')
    saving.value=false
  })
}

</script>

<template>
<div style="margin: auto;max-width: 600px">
  <div style="margin-top: 20px">
    <card :icon="Setting"  v-loading="saving"  title="隐私设置" desc="在这里设置的内容可以被其他人看见，请各位小伙伴注意隐私">
        <div class="checkbox-list">
          <el-checkbox v-model="privacy.phone"
            @change="savePrivacy('phone',privacy.phone)" >公开展示我的手机号</el-checkbox>
          <el-checkbox v-model="privacy.email"
                       @change="savePrivacy('email',privacy.email)">公开展示我的电子邮件</el-checkbox>
          <el-checkbox v-model="privacy.wx"
                       @change="savePrivacy('wx',privacy.wx)">公开展示我的微信号</el-checkbox>
          <el-checkbox v-model="privacy.qq"
                       @change="savePrivacy('qq',privacy.qq)">公开展示我的QQ号</el-checkbox>
          <el-checkbox v-model="privacy.gender"
                       @change="savePrivacy('gender',privacy.gender)">公开展示我的性别</el-checkbox>
        </div>
    </card>
    <card style="margin: 20px 0 " :icon="Setting" title="修改密码" desc="修改密码请在这里操作，请您务必牢记您的密码">
      <el-form    ref="formRef" :rules="rules" :model="from"  @validate="onValidate"
                  label-width="100"  style="margin: 20px">
        <el-form-item label="当前密码" prop="password">
          <el-input   type="password"
              v-model="from.password"  :prefix-icon="Lock" placeholder="当前密码" maxlength="20"/>
        </el-form-item>
        <el-form-item label="新密码"  prop="new_password">
          <el-input  type="password"
              v-model="from.new_password" :prefix-icon="Lock" placeholder="新密码" maxlength="20"/>
        </el-form-item>
        <el-form-item label="重置密码" prop="new_password_repeat">
          <el-input  type="password"
              v-model="from.new_password_repeat" :prefix-icon="Lock" placeholder="重新输入新密码" maxlength="20"/>
        </el-form-item>
        <div style="text-align: center">
          <el-button @click="resetPassword" :icon="Switch" type="success">立即重置密码</el-button>
        </div>
      </el-form>
    </card>
  </div>
</div>
</template>

<style scoped>
.checkbox-list{
  margin: 10px 0 0 10px;
  display: flex;
  flex-direction: column;
}
</style>