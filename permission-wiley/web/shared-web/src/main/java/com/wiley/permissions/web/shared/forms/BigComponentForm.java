package com.wiley.permissions.web.shared.forms;

import com.wiley.permissions.domain.persistence.permissions.CommonWork;
import com.wiley.permissions.domain.persistence.permissions.Component;
import com.wiley.permissions.web.shared.controllers.BaseFormBean;

/**
 *
 * @author smarkoff
 */
public class BigComponentForm
extends BaseFormBean {

	private static final long serialVersionUID = 1L;

	private boolean add1, add2, add3, add4, add5, add6, add7, add8, add9;

	private Component component1 = new Component();
	private Component component2 = new Component();
	private Component component3 = new Component();
	private Component component4 = new Component();
	private Component component5 = new Component();
	private Component component6 = new Component();
	private Component component7 = new Component();
	private Component component8 = new Component();
	private Component component9 = new Component();

	private boolean addChapters;
	private boolean addAppendices;

	private Integer chapterStart, chapterEnd;
	private char appendixStart, appendixEnd;

	private Integer chapterStartSortOrder;
	private Integer appendixStartSortOrder;


	public BigComponentForm() {
		super();

		component1.setCommonWork(new CommonWork());
		component2.setCommonWork(new CommonWork());
		component3.setCommonWork(new CommonWork());
		component4.setCommonWork(new CommonWork());
		component5.setCommonWork(new CommonWork());
		component6.setCommonWork(new CommonWork());
		component7.setCommonWork(new CommonWork());
		component8.setCommonWork(new CommonWork());
		component9.setCommonWork(new CommonWork());

		component1.setName("Front Cover");
		component2.setName("Back Cover");
		component3.setName("Spine");
		component4.setName("Preface");
		component5.setName("Introduction");
		component6.setName("TOC");
		component7.setName("Foreword");
		component8.setName("Afterword");
		component9.setName("Conclusion");
	}

	public void setAdd1(boolean add1) {
		this.add1 = add1;
	}

	public boolean getAdd1() {
		return add1;
	}

	public void setAdd2(boolean add2) {
		this.add2 = add2;
	}

	public boolean getAdd2() {
		return add2;
	}

	public void setAdd3(boolean add3) {
		this.add3 = add3;
	}

	public boolean getAdd3() {
		return add3;
	}

	public void setAdd4(boolean add4) {
		this.add4 = add4;
	}

	public boolean getAdd4() {
		return add4;
	}

	public void setAdd5(boolean add5) {
		this.add5 = add5;
	}

	public boolean getAdd5() {
		return add5;
	}

	public void setAdd6(boolean add6) {
		this.add6 = add6;
	}

	public boolean getAdd6() {
		return add6;
	}

	public void setAdd7(boolean add7) {
		this.add7 = add7;
	}

	public boolean getAdd7() {
		return add7;
	}

	public boolean getAdd8()
	{
		return add8;
	}

	public void setAdd8(boolean add8)
	{
		this.add8 = add8;
	}

	public boolean getAdd9()
	{
		return add9;
	}

	public void setAdd9(boolean add9)
	{
		this.add9 = add9;
	}

	public Component getComponent1() {
		return component1;
	}

	public void setComponent1(Component component1) {
		this.component1 = component1;
	}

	public Component getComponent2() {
		return component2;
	}

	public void setComponent2(Component component2) {
		this.component2 = component2;
	}

	public Component getComponent3() {
		return component3;
	}

	public void setComponent3(Component component3) {
		this.component3 = component3;
	}

	public Component getComponent4() {
		return component4;
	}

	public void setComponent4(Component component4) {
		this.component4 = component4;
	}

	public Component getComponent5() {
		return component5;
	}

	public void setComponent5(Component component5) {
		this.component5 = component5;
	}

	public Component getComponent6() {
		return component6;
	}

	public void setComponent6(Component component6) {
		this.component6 = component6;
	}

	public Component getComponent7() {
		return component7;
	}

	public void setComponent7(Component component7) {
		this.component7 = component7;
	}

	public Component getComponent8()
	{
		return component8;
	}

	public void setComponent8(Component component8)
	{
		this.component8 = component8;
	}

	public Component getComponent9()
	{
		return component9;
	}

	public void setComponent9(Component component9)
	{
		this.component9 = component9;
	}

	public void setAddChapters(boolean addChapters) {
		this.addChapters = addChapters;
	}

	public boolean getAddChapters() {
		return addChapters;
	}

	public void setAddAppendices(boolean addAppendices) {
		this.addAppendices = addAppendices;
	}

	public boolean getAddAppendices() {
		return addAppendices;
	}

	public void setChapterStart(Integer chapterStart) {
		this.chapterStart = chapterStart;
	}

	public Integer getChapterStart() {
		return chapterStart;
	}

	public void setChapterEnd(Integer chapterEnd) {
		this.chapterEnd = chapterEnd;
	}

	public Integer getChapterEnd() {
		return chapterEnd;
	}

	public void setAppendixStart(char appendixStart) {
		this.appendixStart = appendixStart;
	}

	public char getAppendixStart() {
		return appendixStart;
	}

	public void setAppendixEnd(char appendixEnd) {
		this.appendixEnd = appendixEnd;
	}

	public char getAppendixEnd() {
		return appendixEnd;
	}

	public void setChapterStartSortOrder(Integer chapterStartSortOrder) {
		this.chapterStartSortOrder = chapterStartSortOrder;
	}

	public Integer getChapterStartSortOrder() {
		return chapterStartSortOrder;
	}

	public void setAppendixStartSortOrder(Integer appendixStartSortOrder) {
		this.appendixStartSortOrder = appendixStartSortOrder;
	}

	public Integer getAppendixStartSortOrder() {
		return appendixStartSortOrder;
	}
}
