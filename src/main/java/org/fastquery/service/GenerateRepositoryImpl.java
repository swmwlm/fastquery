/*
 * Copyright (c) 2016-2016, fastquery.org and/or its affiliates. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * For more information, please see http://www.fastquery.org/.
 * 
 */

package org.fastquery.service;

import java.util.Set;

import org.apache.log4j.Logger;
import org.fastquery.asm.AsmRepository;
import org.fastquery.core.GenerateRepository;
import org.fastquery.core.Repository;
import org.fastquery.dsm.FastQueryJson;
import org.fastquery.util.LoadPrperties;

/**
 * 这个类在osgi环境中不能调用
 * @author xixifeng (fastquery@126.com)
 */
class GenerateRepositoryImpl extends ClassLoader implements GenerateRepository {

	private static final Logger LOG = Logger.getLogger(GenerateRepositoryImpl.class);
	
	private volatile static GenerateRepositoryImpl instance;

	private GenerateRepositoryImpl() {
		LOG.debug("GenerateRepositoryImpl 已实例化.");
		
		// 装载配置文件
		Set<FastQueryJson> fqPropertie = LoadPrperties.load(new FQueryResourceImpl());
		
		// 批量生成 Repository 的实现类
		Set<String> basePackages = null;
		for (FastQueryJson fQueryPropertie : fqPropertie) {
			basePackages = fQueryPropertie.getBasePackages();
			for (String basePackage : basePackages) {
				try {
					@SuppressWarnings("unchecked")
					Class<? extends Repository> clazz = (Class<? extends Repository>) Class.forName(basePackage);
					generate(clazz);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
					throw new ExceptionInInitializerError(e.getMessage());
				}
			}
		}
	}

	static GenerateRepositoryImpl getInstance() {
		if (instance == null) {
			// 如果在这儿, 可能有两个线程进入了
			synchronized (GenerateRepositoryImpl.class) {
				if (instance == null) { // 这步不能省!
					instance = new GenerateRepositoryImpl();
				}
			}
		}
		return instance;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Repository> T generate(Class<T> repositoryClazz) {

		String name = repositoryClazz.getName()+ suffix;
		
		Class<T> clazz = (Class<T>) findLoadedClass(name);
		
		if(clazz == null) { // 等待生成的类,不存在才能生成.
			byte[] bytes = AsmRepository.generateBytes(repositoryClazz);
			try {
				return (T) defineClass(name,bytes, 0, bytes.length).newInstance();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (ClassFormatError e) {
				e.printStackTrace();
			}
		} else { // 表明已经生成了
			LOG.info(name + "这个类已经生成并装载!");
			try {
				return clazz.newInstance();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Repository> T getProxyRepository(Class<T> clazz) {
		T t = null;
		try {
			t = (T) this.findLoadedClass(clazz.getName() + suffix).newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return t;
	}
}
