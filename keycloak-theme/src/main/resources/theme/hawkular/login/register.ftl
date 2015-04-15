<#--

    Copyright 2015 Red Hat, Inc. and/or its affiliates
    and other contributors as indicated by the @author tags.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->
<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "title">
    ${msg("registerWithTitle",(realm.name!''))}
    <#elseif section = "header">
    ${msg("registerWithTitleHtml",(realm.name!''))}
    <#elseif section = "form">
    <form id="kc-register-form" class="${properties.kcFormClass!}" action="${url.registrationAction}" method="post">
        <#if !realm.registrationEmailAsUsername>
            <div class="${properties.kcFormGroupClass!} ${messagesPerField.printIfExists('username',properties.kcFormGroupErrorClass!)}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="username" class="${properties.kcLabelClass!}">${msg("username")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <input type="text" id="username" class="${properties.kcInputClass!}" name="username" value="${(register.formData.username!'')?html}" />
                </div>
            </div>
        </#if>
        <div class="${properties.kcFormGroupClass!} ${messagesPerField.printIfExists('firstName',properties.kcFormGroupErrorClass!)}">
            <div class="${properties.kcLabelWrapperClass!}">
                <label for="firstName" class="${properties.kcLabelClass!}">${msg("firstName")}</label>
            </div>
            <div class="${properties.kcInputWrapperClass!}">
                <input type="text" id="firstName" class="${properties.kcInputClass!}" name="firstName" value="${(register.formData.firstName!'')?html}" />
            </div>
        </div>

        <div class="${properties.kcFormGroupClass!} ${messagesPerField.printIfExists('lastName',properties.kcFormGroupErrorClass!)}">
            <div class="${properties.kcLabelWrapperClass!}">
                <label for="lastName" class="${properties.kcLabelClass!}">${msg("lastName")}</label>
            </div>
            <div class="${properties.kcInputWrapperClass!}">
                <input type="text" id="lastName" class="${properties.kcInputClass!}" name="lastName" value="${(register.formData.lastName!'')?html}" />
            </div>
        </div>

        <div class="${properties.kcFormGroupClass!} ${messagesPerField.printIfExists('email',properties.kcFormGroupErrorClass!)}">
            <div class="${properties.kcLabelWrapperClass!}">
                <label for="email" class="${properties.kcLabelClass!}">${msg("email")}</label>
            </div>
            <div class="${properties.kcInputWrapperClass!}">
                <input type="text" id="email" class="${properties.kcInputClass!}" name="email" value="${(register.formData.email!'')?html}" />
            </div>
        </div>

        <div class="${properties.kcFormGroupClass!} ${messagesPerField.printIfExists('password',properties.kcFormGroupErrorClass!)}">
            <div class="${properties.kcLabelWrapperClass!}">
                <label for="password" class="${properties.kcLabelClass!}">${msg("password")}</label>
            </div>
            <div class="${properties.kcInputWrapperClass!}">
                <input type="password" id="password" class="${properties.kcInputClass!}" name="password" />
            </div>
        </div>

        <div class="${properties.kcFormGroupClass!} ${messagesPerField.printIfExists('password-confirm',properties.kcFormGroupErrorClass!)}">
            <div class="${properties.kcLabelWrapperClass!}">
                <label for="password-confirm" class="${properties.kcLabelClass!}">${msg("passwordConfirm")}</label>
            </div>
            <div class="${properties.kcInputWrapperClass!}">
                <input type="password" id="password-confirm" class="${properties.kcInputClass!}" name="password-confirm" />
            </div>
        </div>

        <div class="${properties.kcFormGroupClass!}">
            <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                <div class="${properties.kcFormOptionsWrapperClass!}">
                    <span><a href="${url.loginUrl}">${msg("backToLogin")}</a></span>
                </div>
            </div>

            <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                <input class="btn btn-primary btn-lg" type="submit" value="${msg("doRegister")}"/>
            </div>
        </div>
    </form>
    </#if>
</@layout.registrationLayout>