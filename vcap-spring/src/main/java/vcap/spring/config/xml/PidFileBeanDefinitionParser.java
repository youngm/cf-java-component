/*
 *   Copyright (c) 2013 Mike Heath.  All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package vcap.spring.config.xml;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import vcap.spring.PidFileFactory;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class PidFileBeanDefinitionParser implements BeanDefinitionParser {
	@Override
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		final String pidFileName = element.getAttribute("file");

		final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(PidFileFactory.class);
		builder.addConstructorArgValue(pidFileName);
		final AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
		beanDefinition.setLazyInit(false);

		final String beanId = parserContext.getReaderContext().generateBeanName(beanDefinition);

		parserContext.registerBeanComponent(new BeanComponentDefinition(beanDefinition, beanId));

		return null;
	}
}
