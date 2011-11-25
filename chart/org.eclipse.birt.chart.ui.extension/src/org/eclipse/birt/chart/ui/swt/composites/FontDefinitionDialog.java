/***********************************************************************
 * Copyright (c) 2004 Actuate Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Actuate Corporation - initial API and implementation
 ***********************************************************************/

package org.eclipse.birt.chart.ui.swt.composites;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.birt.chart.model.ChartWithAxes;
import org.eclipse.birt.chart.model.attribute.ColorDefinition;
import org.eclipse.birt.chart.model.attribute.FontDefinition;
import org.eclipse.birt.chart.model.attribute.HorizontalAlignment;
import org.eclipse.birt.chart.model.attribute.Orientation;
import org.eclipse.birt.chart.model.attribute.VerticalAlignment;
import org.eclipse.birt.chart.model.attribute.impl.FontDefinitionImpl;
import org.eclipse.birt.chart.ui.extension.i18n.Messages;
import org.eclipse.birt.chart.ui.swt.composites.FontDefinitionComposite.IFontDefinitionDialog;
import org.eclipse.birt.chart.ui.swt.wizard.ChartWizardContext;
import org.eclipse.birt.chart.ui.util.ChartHelpContextIds;
import org.eclipse.birt.chart.ui.util.ChartUIExtensionUtil;
import org.eclipse.birt.chart.ui.util.ChartUIUtil;
import org.eclipse.birt.chart.ui.util.UIHelper;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Actuate Corporation
 * 
 */
public class FontDefinitionDialog extends TrayDialog implements
		IFontDefinitionDialog,
		SelectionListener,
		Listener,
		IAngleChangeListener,
		FocusListener
{

	private transient FontDefinition fdCurrent = null;

	private transient ColorDefinition cdCurrent = null;

	private transient FontDefinition fdBackup = null;

	private transient ColorDefinition cdBackup = null;

	private transient Composite cmpContent = null;

	private transient Combo cmbFontNames = null;

	private transient Combo cmbFontSizes = null;

	private transient FillChooserComposite fccColor = null;

	private transient Button btnATopLeft = null;

	private transient Button btnATopCenter = null;

	private transient Button btnATopRight = null;

	private transient Button btnACenterLeft = null;

	private transient Button btnACenter = null;

	private transient Button btnACenterRight = null;

	private transient Button btnABottomLeft = null;

	private transient Button btnABottomCenter = null;

	private transient Button btnABottomRight = null;

	private transient Button btnBold = null;

	private transient Button btnItalic = null;

	private transient Button btnUnderline = null;

	private transient Button btnStrikethru = null;

	private transient AngleSelectorComposite ascRotation = null;

	private transient IntegerSpinControl iscRotation = null;

	private transient FontCanvas fcPreview = null;

	private transient boolean isAlignmentEnabled = true;

	private transient List<Button> listAlignmentButtons = new ArrayList<Button>( 9 );

	private transient ChartWizardContext wizardContext;

	private Button btnAlignmentAuto;

	private Button btnAutoRotation;

	private static final String[] FONT_SIZE = new String[]{
			ChartUIUtil.FONT_AUTO,
			"9", "10", "12", "14", "16", "18", "24", "36" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
	};

	public FontDefinitionDialog( Shell shellParent,
			ChartWizardContext wizardContext, FontDefinition fdCurrent,
			ColorDefinition cdCurrent, boolean isAlignmentEnabled )
	{
		super( shellParent );

		this.isAlignmentEnabled = isAlignmentEnabled;
		this.wizardContext = wizardContext;
		this.fdCurrent = fdCurrent == null ? FontDefinitionImpl.createEmpty( )
				: fdCurrent.copyInstance( );
		this.cdCurrent = cdCurrent == null ? null : cdCurrent.copyInstance( );
		this.fdBackup = fdCurrent == null ? null : fdCurrent.copyInstance( );
		this.cdBackup = this.cdCurrent == null ? null
				: cdCurrent.copyInstance( );
	}

	protected void setShellStyle( int newShellStyle )
	{
		super.setShellStyle( newShellStyle
				| SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL );
	}

	protected Control createContents( Composite parent )
	{
		ChartUIUtil.bindHelp( parent, ChartHelpContextIds.DIALOG_FONT_EDITOR );
		getShell( ).setText( Messages.getString( "FontDefinitionDialog.Title.FontDescriptor" ) ); //$NON-NLS-1$
		return super.createContents( parent );
	}

	protected Control createDialogArea( Composite parent )
	{
		GridLayout glContent = new GridLayout( );
		glContent.verticalSpacing = 5;
		glContent.horizontalSpacing = 5;
		glContent.marginHeight = 7;
		glContent.marginWidth = 7;
		glContent.numColumns = 9;

		cmpContent = new Composite( parent, SWT.NONE );
		cmpContent.setLayout( glContent );
		cmpContent.setLayoutData( new GridData( GridData.FILL_BOTH ) );

		Label lblFont = new Label( cmpContent, SWT.NONE );
		GridData gdLFont = new GridData( );
		lblFont.setLayoutData( gdLFont );
		lblFont.setText( Messages.getString( "FontDefinitionDialog.Lbl.Font" ) ); //$NON-NLS-1$

		cmbFontNames = new Combo( cmpContent, SWT.DROP_DOWN | SWT.READ_ONLY );
		GridData gdCMBFontNames = new GridData( GridData.FILL_HORIZONTAL );
		gdCMBFontNames.horizontalSpan = 8;
		cmbFontNames.setLayoutData( gdCMBFontNames );
		cmbFontNames.addSelectionListener( this );
		cmbFontNames.setVisibleItemCount( 30 );

		Label lblSize = new Label( cmpContent, SWT.NONE );
		GridData gdLSize = new GridData( );
		lblSize.setLayoutData( gdLSize );
		lblSize.setText( Messages.getString( "FontDefinitionDialog.Lbl.Size" ) ); //$NON-NLS-1$

		cmbFontSizes = new Combo( cmpContent, SWT.NONE );
		{
			cmbFontSizes.setItems( FONT_SIZE );
			cmbFontSizes.setText( fdCurrent.isSetSize( )
					? String.valueOf( (int) fdCurrent.getSize( ) )
					: ChartUIUtil.FONT_AUTO );
			GridData gdISCFontSizes = new GridData( GridData.FILL_HORIZONTAL );
			gdISCFontSizes.horizontalSpan = 3;
			cmbFontSizes.setLayoutData( gdISCFontSizes );
			cmbFontSizes.addSelectionListener( this );
			cmbFontSizes.addFocusListener( this );
			cmbFontSizes.setVisibleItemCount( 30 );
		}

		Label lblForeground = new Label( cmpContent, SWT.NONE );
		GridData gdLForeground = new GridData( );
		gdLForeground.horizontalSpan = 2;
		gdLForeground.horizontalIndent = 40;
		lblForeground.setLayoutData( gdLForeground );
		lblForeground.setText( Messages.getString( "FontDefinitionDialog.Lbl.Color" ) ); //$NON-NLS-1$

		fccColor = new FillChooserComposite( cmpContent,
				SWT.NONE,
				wizardContext,
				cdCurrent,
				false,
				false,
				true,
				false,
				false,
				false );
		{
			GridData gdFCCColor = new GridData( GridData.FILL_HORIZONTAL );
			gdFCCColor.horizontalSpan = 3;
			fccColor.setLayoutData( gdFCCColor );
			fccColor.addListener( this );
		}

		createFontStylePanel( );

		if ( isAlignmentEnabled )
		{
			createAlignmentPanel( );
		}

		createRotationPanel( );

		populateLists( );
		updatePreview( );

		return cmpContent;
	}

	protected void cancelPressed( )
	{
		this.fdCurrent = this.fdBackup;
		this.cdCurrent = this.cdBackup;
		super.cancelPressed( );
	}

	private void createFontStylePanel( )
	{
		Label lblStyle = new Label( cmpContent, SWT.NONE );
		lblStyle.setText( Messages.getString( "FontDefinitionDialog.Lbl.Style" ) ); //$NON-NLS-1$

		Composite cmpFontStyle = new Composite( cmpContent, SWT.NONE );
		{
			GridLayout layout = new GridLayout( 4, false );
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			cmpFontStyle.setLayout( layout );
			GridData gd = new GridData( );
			gd.horizontalSpan = 8;
			cmpFontStyle.setLayoutData( gd );
		}

		btnBold = new Button( cmpFontStyle, SWT.TOGGLE );
		{
			GridData gdBBold = new GridData( );
			btnBold.setLayoutData( gdBBold );
			btnBold.setImage( UIHelper.getImage( "icons/obj16/fnt_style_bold.gif" ) ); //$NON-NLS-1$
			btnBold.setToolTipText( Messages.getString( "FontDefinitionDialog.Tooltip.Bold" ) ); //$NON-NLS-1$
			btnBold.addSelectionListener( this );
			btnBold.setSelection( fdCurrent.isSetBold( ) && fdCurrent.isBold( ) );
		}

		btnItalic = new Button( cmpFontStyle, SWT.TOGGLE );
		{
			GridData gdBItalic = new GridData( );
			btnItalic.setLayoutData( gdBItalic );
			btnItalic.setImage( UIHelper.getImage( "icons/obj16/fnt_style_italic.gif" ) ); //$NON-NLS-1$
			btnItalic.setToolTipText( Messages.getString( "FontDefinitionDialog.Tooltip.Italic" ) ); //$NON-NLS-1$
			btnItalic.addSelectionListener( this );
			btnItalic.setSelection( fdCurrent.isSetItalic( )
					&& fdCurrent.isItalic( ) );
		}

		btnUnderline = new Button( cmpFontStyle, SWT.TOGGLE );
		{
			GridData gdBUnderline = new GridData( );
			btnUnderline.setLayoutData( gdBUnderline );
			btnUnderline.setImage( UIHelper.getImage( "icons/obj16/fnt_style_underline.gif" ) ); //$NON-NLS-1$
			btnUnderline.setToolTipText( Messages.getString( "FontDefinitionDialog.Tooltip.Underline" ) ); //$NON-NLS-1$
			btnUnderline.addSelectionListener( this );
			btnUnderline.setSelection( fdCurrent.isSetUnderline( )
					&& fdCurrent.isUnderline( ) );
		}

		btnStrikethru = new Button( cmpFontStyle, SWT.TOGGLE );
		{
			btnStrikethru.setImage( UIHelper.getImage( "icons/obj16/fnt_style_Sthrough.gif" ) ); //$NON-NLS-1$
			btnStrikethru.setToolTipText( Messages.getString( "FontDefinitionDialog.Tooltip.Strikethrough" ) ); //$NON-NLS-1$
			btnStrikethru.addSelectionListener( this );
			btnStrikethru.setSelection( fdCurrent.isSetStrikethrough( )
					&& fdCurrent.isStrikethrough( ) );
		}

	}

	private void createAlignmentPanel( )
	{
		new Label( cmpContent, SWT.NONE ).setText( Messages.getString( "FontDefinitionDialog.Lbl.Alignment" ) ); //$NON-NLS-1$

		Composite cmpAlignment = new Composite( cmpContent, SWT.NONE );
		{
			GridData gdCMPAlignment = new GridData( GridData.FILL_HORIZONTAL );
			gdCMPAlignment.horizontalSpan = 8;
			cmpAlignment.setLayoutData( gdCMPAlignment );
			GridLayout glAlignment = new GridLayout( 12, false );
			glAlignment.marginWidth = 2;
			glAlignment.marginHeight = 0;
			cmpAlignment.setLayout( glAlignment );
		}
		
		btnAlignmentAuto = new Button(cmpAlignment, SWT.CHECK );
		btnAlignmentAuto.setText( ChartUIExtensionUtil.getAutoMessage( ) );
		btnAlignmentAuto.addSelectionListener( this );
		
		btnATopLeft = createAlighmentButton( cmpAlignment );
		btnATopCenter = createAlighmentButton( cmpAlignment );
		btnATopRight = createAlighmentButton( cmpAlignment );
		btnACenterLeft = createAlighmentButton( cmpAlignment );
		btnACenter = createAlighmentButton( cmpAlignment );
		btnACenterRight = createAlighmentButton( cmpAlignment );
		btnABottomLeft = createAlighmentButton( cmpAlignment );
		btnABottomCenter = createAlighmentButton( cmpAlignment );
		btnABottomRight = createAlighmentButton( cmpAlignment );
		
		if ( isFlippedAxes( ) )
		{
			btnATopLeft.setToolTipText( Messages.getString( "FontDefinitionDialog.Tooltip.AlignBottomRight" ) ); //$NON-NLS-1$
			btnATopLeft.setImage( UIHelper.getImage( "icons/obj28/alignmentbottomright.gif" ) ); //$NON-NLS-1$
			btnATopLeft.getImage( )
					.setBackground( btnATopLeft.getBackground( ) );
			
			btnATopCenter.setToolTipText( Messages.getString( "FontDefinitionDialog.Tooltip.AlignCenterRight" ) ); //$NON-NLS-1$
			btnATopCenter.setImage( UIHelper.getImage( "icons/obj28/alignmentcenterright.gif" ) ); //$NON-NLS-1$
			btnATopCenter.getImage( )
					.setBackground( btnATopCenter.getBackground( ) );
			
			btnATopRight.setToolTipText( Messages.getString( "FontDefinitionDialog.Tooltip.AlignTopRight" ) ); //$NON-NLS-1$
			btnATopRight.setImage( UIHelper.getImage( "icons/obj28/alignmenttopright.gif" ) ); //$NON-NLS-1$
			btnATopRight.getImage( ).setBackground( btnATopRight.getBackground( ) );
			
			createSeparator( cmpAlignment );
			
			btnACenterLeft.setToolTipText( Messages.getString( "FontDefinitionDialog.Tooltip.AlignBottomCenter" ) ); //$NON-NLS-1$
			btnACenterLeft.setImage( UIHelper.getImage( "icons/obj28/alignmentbottomcenter.gif" ) ); //$NON-NLS-1$
			btnACenterLeft.getImage( )
					.setBackground( btnACenterLeft.getBackground( ) );

			btnACenter.setToolTipText( Messages.getString( "FontDefinitionDialog.Tooltip.AlignCenter" ) ); //$NON-NLS-1$
			btnACenter.setImage( UIHelper.getImage( "icons/obj28/alignmentcenter.gif" ) ); //$NON-NLS-1$
			btnACenter.getImage( ).setBackground( btnACenter.getBackground( ) );

			btnACenterRight.setToolTipText( Messages.getString( "FontDefinitionDialog.Tooltip.AlignTopCenter" ) ); //$NON-NLS-1$
			btnACenterRight.setImage( UIHelper.getImage( "icons/obj28/alignmenttopcenter.gif" ) ); //$NON-NLS-1$
			btnACenterRight.getImage( )
					.setBackground( btnACenterRight.getBackground( ) );

			createSeparator( cmpAlignment );

			btnABottomLeft.setToolTipText( Messages.getString( "FontDefinitionDialog.Tooltip.AlignBottomLeft" ) ); //$NON-NLS-1$
			btnABottomLeft.setImage( UIHelper.getImage( "icons/obj28/alignmentbottomleft.gif" ) ); //$NON-NLS-1$
			btnABottomLeft.getImage( )
					.setBackground( btnABottomLeft.getBackground( ) );

			btnABottomCenter.setToolTipText( Messages.getString( "FontDefinitionDialog.Tooltip.AlignCenterLeft" ) ); //$NON-NLS-1$
			btnABottomCenter.setImage( UIHelper.getImage( "icons/obj28/alignmentcenterleft.gif" ) ); //$NON-NLS-1$
			btnABottomCenter.getImage( )
					.setBackground( btnABottomCenter.getBackground( ) );

			btnABottomRight.setToolTipText( Messages.getString( "FontDefinitionDialog.Tooltip.AlignTopLeft" ) ); //$NON-NLS-1$
			btnABottomRight.setImage( UIHelper.getImage( "icons/obj28/alignmenttopleft.gif" ) ); //$NON-NLS-1$
			btnABottomRight.getImage( )
					.setBackground( btnABottomRight.getBackground( ) );		
		}
		else
		{
			btnATopLeft.setToolTipText( Messages.getString( "FontDefinitionDialog.Tooltip.AlignTopLeft" ) ); //$NON-NLS-1$
			btnATopLeft.setImage( UIHelper.getImage( "icons/obj28/alignmenttopleft.gif" ) ); //$NON-NLS-1$
			btnATopLeft.getImage( )
					.setBackground( btnATopLeft.getBackground( ) );
			
			btnATopCenter.setToolTipText( Messages.getString( "FontDefinitionDialog.Tooltip.AlignTopCenter" ) ); //$NON-NLS-1$
			btnATopCenter.setImage( UIHelper.getImage( "icons/obj28/alignmenttopcenter.gif" ) ); //$NON-NLS-1$
			btnATopCenter.getImage( )
					.setBackground( btnATopCenter.getBackground( ) );
			
			btnATopRight.setToolTipText( Messages.getString( "FontDefinitionDialog.Tooltip.AlignTopRight" ) ); //$NON-NLS-1$
			btnATopRight.setImage( UIHelper.getImage( "icons/obj28/alignmenttopright.gif" ) ); //$NON-NLS-1$
			btnATopRight.getImage( ).setBackground( btnATopRight.getBackground( ) );
			
			createSeparator( cmpAlignment );
			
			btnACenterLeft.setToolTipText( Messages.getString( "FontDefinitionDialog.Tooltip.AlignCenterLeft" ) ); //$NON-NLS-1$
			btnACenterLeft.setImage( UIHelper.getImage( "icons/obj28/alignmentcenterleft.gif" ) ); //$NON-NLS-1$
			btnACenterLeft.getImage( )
					.setBackground( btnACenterLeft.getBackground( ) );

			btnACenter.setToolTipText( Messages.getString( "FontDefinitionDialog.Tooltip.AlignCenter" ) ); //$NON-NLS-1$
			btnACenter.setImage( UIHelper.getImage( "icons/obj28/alignmentcenter.gif" ) ); //$NON-NLS-1$
			btnACenter.getImage( ).setBackground( btnACenter.getBackground( ) );

			btnACenterRight.setToolTipText( Messages.getString( "FontDefinitionDialog.Tooltip.AlignCenterRight" ) ); //$NON-NLS-1$
			btnACenterRight.setImage( UIHelper.getImage( "icons/obj28/alignmentcenterright.gif" ) ); //$NON-NLS-1$
			btnACenterRight.getImage( )
					.setBackground( btnACenterRight.getBackground( ) );

			createSeparator( cmpAlignment );

			btnABottomLeft.setToolTipText( Messages.getString( "FontDefinitionDialog.Tooltip.AlignBottomLeft" ) ); //$NON-NLS-1$
			btnABottomLeft.setImage( UIHelper.getImage( "icons/obj28/alignmentbottomleft.gif" ) ); //$NON-NLS-1$
			btnABottomLeft.getImage( )
					.setBackground( btnABottomLeft.getBackground( ) );

			btnABottomCenter.setToolTipText( Messages.getString( "FontDefinitionDialog.Tooltip.AlignBottomCenter" ) ); //$NON-NLS-1$
			btnABottomCenter.setImage( UIHelper.getImage( "icons/obj28/alignmentbottomcenter.gif" ) ); //$NON-NLS-1$
			btnABottomCenter.getImage( )
					.setBackground( btnABottomCenter.getBackground( ) );

			btnABottomRight.setToolTipText( Messages.getString( "FontDefinitionDialog.Tooltip.AlignBottomRight" ) ); //$NON-NLS-1$
			btnABottomRight.setImage( UIHelper.getImage( "icons/obj28/alignmentbottomright.gif" ) ); //$NON-NLS-1$
			btnABottomRight.getImage( )
					.setBackground( btnABottomRight.getBackground( ) );	
		}
		btnAlignmentAuto.setSelection( !isSetAlignment( ) );
		if ( btnAlignmentAuto.getSelection( ) )
		{
			disableAlignmentBtns( );
		}
	}

	protected boolean isSetAlignment( )
	{
		return fdCurrent.getAlignment( ).isSetHorizontalAlignment( ) || fdCurrent.getAlignment( ).isSetVerticalAlignment( );
	}

	private Button createAlighmentButton( Composite parent )
	{
		Button button = new Button( parent, SWT.TOGGLE );
		GridData gd = new GridData( );
		gd.widthHint = 32;
		gd.heightHint = 32;
		button.setLayoutData( gd );
		button.addSelectionListener( this );
		listAlignmentButtons.add( button );
		return button;
	}

	private void selectAllToggleButtons( boolean selection )
	{
		for ( int i = 0; i < listAlignmentButtons.size( ); i++ )
		{
			listAlignmentButtons.get( i ).setSelection( selection );
		}
	}

	private void createSeparator( Composite parent )
	{
		Label lable = new Label( parent, SWT.NONE );
		lable.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
	}

	private void createRotationPanel( )
	{
		Composite cmpRotation = new Composite( cmpContent, SWT.NONE );
		{
			GridLayout layout = new GridLayout( 3, false );
			layout.verticalSpacing = 5;
			layout.horizontalSpacing = 5;
			layout.marginHeight = 7;
			layout.marginWidth = 7;
			cmpRotation.setLayout( layout );
			GridData gd = new GridData( GridData.FILL_BOTH );
			gd.horizontalSpan = 3;
			gd.heightHint = 150;
			cmpRotation.setLayoutData( gd );
		}

		Label lblRotation = new Label( cmpRotation, SWT.NONE );
		{
			lblRotation.setLayoutData( new GridData( GridData.VERTICAL_ALIGN_BEGINNING ) );
			lblRotation.setText( Messages.getString( "FontDefinitionDialog.Lbl.Rotation" ) ); //$NON-NLS-1$
		}

		ascRotation = new AngleSelectorComposite( cmpRotation,
				SWT.BORDER,
				ChartUIUtil.getFontRotation( fdCurrent ),
				Display.getCurrent( ).getSystemColor( SWT.COLOR_WHITE ) );
		GridData gdASCRotation = new GridData( GridData.FILL_BOTH );
		gdASCRotation.horizontalSpan = 2;
		ascRotation.setLayoutData( gdASCRotation );
		ascRotation.setAngleChangeListener( this );

		Label lblDegree = new Label( cmpRotation, SWT.NONE );
		{
			lblDegree.setLayoutData( new GridData( ) );
			lblDegree.setText( Messages.getString( "FontDefinitionDialog.Label.Degree" ) ); //$NON-NLS-1$
		}

		iscRotation = new IntegerSpinControl( cmpRotation,
				SWT.NONE,
				ChartUIUtil.getFontRotation( fdCurrent ) );
		GridData gdISCRotation = new GridData( GridData.FILL_HORIZONTAL );
		gdISCRotation.horizontalSpan = 1;
		gdISCRotation.minimumWidth = 40;
		iscRotation.setLayoutData( gdISCRotation );
		iscRotation.setMinimum( -90 );
		iscRotation.setMaximum( 90 );
		iscRotation.setIncrement( 1 );
		iscRotation.addListener( this );
		
		btnAutoRotation = new Button( cmpRotation, SWT.CHECK );
		btnAutoRotation.setText( ChartUIExtensionUtil.getAutoMessage( ) );
		btnAutoRotation.addSelectionListener( this );
		btnAutoRotation.setSelection( fdCurrent == null || !fdCurrent.isSetRotation( ) );
		iscRotation.setEnabled( !btnAutoRotation.getSelection( ) );
		ascRotation.setEnabled( !btnAutoRotation.getSelection( ) );
		
		Label lblPreview = new Label( cmpContent, SWT.NONE );
		{
			lblPreview.setText( Messages.getString( "FontDefinitionDialog.Lbl.Preview" ) ); //$NON-NLS-1$
			lblPreview.setLayoutData( new GridData( GridData.VERTICAL_ALIGN_BEGINNING ) );
		}

		FillLayout flPreview = new FillLayout( );
		flPreview.marginHeight = 2;
		flPreview.marginWidth = 3;
		Composite grpPreview = new Composite( cmpContent, SWT.NONE );
		GridData gdGRPPreview = new GridData( GridData.FILL_BOTH );
		gdGRPPreview.horizontalSpan = 4;
		grpPreview.setLayoutData( gdGRPPreview );
		grpPreview.setLayout( flPreview );

		fcPreview = new FontCanvas( grpPreview,
				SWT.BORDER,
				fdCurrent,
				cdCurrent,
				true,
				true,
				true );

	}

	private void populateLists( )
	{
		// Populate font names list
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment( );
		String[] saFontNames = ge.getAvailableFontFamilyNames( );
		String currentFont = ChartUIUtil.getFontName( fdCurrent );
		cmbFontNames.add( ChartUIUtil.FONT_AUTO );
		if ( ChartUIUtil.FONT_AUTO.equals( currentFont ) )
		{
			cmbFontNames.select( 0 );
		}
		for ( int iC = 0; iC < saFontNames.length; iC++ )
		{
			cmbFontNames.add( saFontNames[iC] );
			if ( saFontNames[iC].equalsIgnoreCase( currentFont ) )
			{
				cmbFontNames.select( iC + 1 );
			}
		}
		if ( cmbFontNames.getSelectionIndex( ) == -1 )
		{
			cmbFontNames.select( 0 );
		}

		// Select alignment button
		if ( isAlignmentEnabled
				&& fdCurrent.getAlignment( ) != null
				&& fdCurrent.getAlignment( ).isSetHorizontalAlignment( )
				&& fdCurrent.getAlignment( ).isSetVerticalAlignment( ) )
		{
			HorizontalAlignment ha = fdCurrent.getAlignment( )
					.getHorizontalAlignment( );
			VerticalAlignment va = fdCurrent.getAlignment( )
					.getVerticalAlignment( );
			if ( HorizontalAlignment.LEFT_LITERAL.equals( ha ) )
			{
				if ( VerticalAlignment.TOP_LITERAL.equals( va ) )
				{
					btnATopLeft.setSelection( true );
				}
				else if ( VerticalAlignment.BOTTOM_LITERAL.equals( va ) )
				{
					btnABottomLeft.setSelection( true );
				}
				else
				{
					btnACenterLeft.setSelection( true );
				}
			}
			else if ( HorizontalAlignment.RIGHT_LITERAL.equals( ha ) )
			{
				if ( VerticalAlignment.TOP_LITERAL.equals( va ) )
				{
					btnATopRight.setSelection( true );
				}
				else if ( VerticalAlignment.BOTTOM_LITERAL.equals( va ) )
				{
					btnABottomRight.setSelection( true );
				}
				else
				{
					btnACenterRight.setSelection( true );
				}
			}
			else
			{
				if ( VerticalAlignment.TOP_LITERAL.equals( va ) )
				{
					btnATopCenter.setSelection( true );
				}
				else if ( VerticalAlignment.BOTTOM_LITERAL.equals( va ) )
				{
					btnABottomCenter.setSelection( true );
				}
				else
				{
					btnACenter.setSelection( true );
				}
			}
		}
	}

	private void updatePreview( )
	{
		FontDefinition fd = fdCurrent.copyInstance( );
		ChartUIUtil.getFlippedAlignment( fd.getAlignment( ), isFlippedAxes( ) );
		fcPreview.setFontDefinition( fd );
		fcPreview.redraw( );
	}

	public FontDefinition getFontDefinition( )
	{
		return this.fdCurrent;
	}

	public ColorDefinition getFontColor( )
	{
		return cdCurrent == null
				|| cdCurrent.isSetTransparency( )
				&& cdCurrent.getTransparency( ) == 0 ? null : cdCurrent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	public void widgetSelected( SelectionEvent e )
	{
		Object oSource = e.getSource( );

		// Handle with alignment buttons
		if ( listAlignmentButtons.contains( oSource ) )
		{
			if ( !( (Button) oSource ).getSelection( ) )
			{
				// Keep the selection to restrict at least one selection
				// ( (Button) oSource ).setSelection( true );
				fdCurrent.getAlignment( ).unsetHorizontalAlignment( );
				fdCurrent.getAlignment( ).unsetVerticalAlignment( );
				updatePreview( );
				return;
			}
			selectAllToggleButtons( false );
			( (Button) oSource ).setSelection( true );
		}
		else if ( e.widget == btnAlignmentAuto )
		{
			if ( btnAlignmentAuto.getSelection( ) )
			{
				disableAlignmentBtns( );
				fdCurrent.getAlignment( ).unsetHorizontalAlignment( );
				fdCurrent.getAlignment( ).unsetVerticalAlignment( );
			}
			else
			{
				for ( int i = 0; i < listAlignmentButtons.size( ); i++ )
				{
					listAlignmentButtons.get( i ).setEnabled( true );
				}
			}
			updatePreview( );
		}
		else if ( e.widget == btnAutoRotation )
		{
			if ( btnAutoRotation.getSelection( ) )
			{
				iscRotation.setEnabled( false );
				ascRotation.setEnabled( false );
				fdCurrent.unsetRotation( );	
			}
			else
			{
				iscRotation.setEnabled( true );
				ascRotation.setEnabled( true );
				fdCurrent.setRotation( iscRotation.getValue( ) );
			}
			iscRotation.setValue( ChartUIUtil.getFontRotation( fdCurrent ) );
			ascRotation.setAngle( ChartUIUtil.getFontRotation( fdCurrent ) );
			ascRotation.redraw( );
			updatePreview( );
		}
		else if ( oSource.equals( btnBold ) )
		{
			if ( btnBold.getSelection( ) )
			{
				fdCurrent.setBold( btnBold.getSelection( ) );
			}
			else
			{
				fdCurrent.unsetBold( );
			}
			updatePreview( );
		}
		else if ( oSource.equals( btnItalic ) )
		{
			if ( btnItalic.getSelection( ) )
			{
				fdCurrent.setItalic( btnItalic.getSelection( ) );
			}
			else
			{
				fdCurrent.unsetItalic( );
			}
			updatePreview( );
		}
		else if ( oSource.equals( btnUnderline ) )
		{
			if ( btnUnderline.getSelection( ) )
			{
				fdCurrent.setUnderline( btnUnderline.getSelection( ) );
			}
			else
			{
				fdCurrent.unsetUnderline( );
			}
			updatePreview( );
		}
		else if ( oSource.equals( btnStrikethru ) )
		{
			if ( btnStrikethru.getSelection( ) )
			{
				fdCurrent.setStrikethrough( btnStrikethru.getSelection( ) );
			}
			else
			{
				fdCurrent.unsetStrikethrough( );
			}
			updatePreview( );
		}
		else if ( oSource.equals( cmbFontNames ) )
		{
			if ( cmbFontNames.getText( ).equals( ChartUIUtil.FONT_AUTO ) )
			{
				fdCurrent.setName( null );
			}
			else
			{
				fdCurrent.setName( cmbFontNames.getText( ) );
			}
			updatePreview( );
		}
		else if ( oSource.equals( cmbFontSizes ) )
		{
			handleFontSize( );
		}
		else if ( oSource.equals( this.btnATopLeft ) )
		{
			fdCurrent.getAlignment( )
					.setHorizontalAlignment( HorizontalAlignment.LEFT_LITERAL );
			fdCurrent.getAlignment( )
					.setVerticalAlignment( VerticalAlignment.TOP_LITERAL );
			updatePreview( );
		}
		else if ( oSource.equals( this.btnACenterLeft ) )
		{
			fdCurrent.getAlignment( )
					.setHorizontalAlignment( HorizontalAlignment.LEFT_LITERAL );
			fdCurrent.getAlignment( )
					.setVerticalAlignment( VerticalAlignment.CENTER_LITERAL );
			updatePreview( );
		}
		else if ( oSource.equals( this.btnABottomLeft ) )
		{
			fdCurrent.getAlignment( )
					.setHorizontalAlignment( HorizontalAlignment.LEFT_LITERAL );
			fdCurrent.getAlignment( )
					.setVerticalAlignment( VerticalAlignment.BOTTOM_LITERAL );
			updatePreview( );
		}
		else if ( oSource.equals( this.btnATopCenter ) )
		{
			fdCurrent.getAlignment( )
					.setHorizontalAlignment( HorizontalAlignment.CENTER_LITERAL );
			fdCurrent.getAlignment( )
					.setVerticalAlignment( VerticalAlignment.TOP_LITERAL );
			updatePreview( );
		}
		else if ( oSource.equals( this.btnACenter ) )
		{
			fdCurrent.getAlignment( )
					.setHorizontalAlignment( HorizontalAlignment.CENTER_LITERAL );
			fdCurrent.getAlignment( )
					.setVerticalAlignment( VerticalAlignment.CENTER_LITERAL );
			updatePreview( );
		}
		else if ( oSource.equals( this.btnABottomCenter ) )
		{
			fdCurrent.getAlignment( )
					.setHorizontalAlignment( HorizontalAlignment.CENTER_LITERAL );
			fdCurrent.getAlignment( )
					.setVerticalAlignment( VerticalAlignment.BOTTOM_LITERAL );
			updatePreview( );
		}
		else if ( oSource.equals( this.btnATopRight ) )
		{
			fdCurrent.getAlignment( )
					.setHorizontalAlignment( HorizontalAlignment.RIGHT_LITERAL );
			fdCurrent.getAlignment( )
					.setVerticalAlignment( VerticalAlignment.TOP_LITERAL );
			updatePreview( );
		}
		else if ( oSource.equals( this.btnACenterRight ) )
		{
			fdCurrent.getAlignment( )
					.setHorizontalAlignment( HorizontalAlignment.RIGHT_LITERAL );
			fdCurrent.getAlignment( )
					.setVerticalAlignment( VerticalAlignment.CENTER_LITERAL );
			updatePreview( );
		}
		else if ( oSource.equals( this.btnABottomRight ) )
		{
			fdCurrent.getAlignment( )
					.setHorizontalAlignment( HorizontalAlignment.RIGHT_LITERAL );
			fdCurrent.getAlignment( )
					.setVerticalAlignment( VerticalAlignment.BOTTOM_LITERAL );
			updatePreview( );
		}
	}

	protected void disableAlignmentBtns( )
	{
		for ( int i = 0; i < listAlignmentButtons.size( ); i++ )
		{
			listAlignmentButtons.get( i ).setSelection( false );
			listAlignmentButtons.get( i ).setEnabled( false );
		}
	}

	public void handleEvent( Event e )
	{
		if ( e.widget.equals( iscRotation ) )
		{
			fdCurrent.setRotation( iscRotation.getValue( ) );
			ascRotation.setAngle( iscRotation.getValue( ) );
			ascRotation.redraw( );
			// TODO: Enable this if support for rotated text is added to
			// fontcanvas
			updatePreview( );
		}
		else if ( e.widget.equals( fccColor ) )
		{
			if ( e.type == FillChooserComposite.FILL_CHANGED_EVENT )
			{
				cdCurrent = (ColorDefinition) fccColor.getFill( );
				// if ( cdCurrent == null )
				// {
				// cdCurrent = ColorDefinitionImpl.TRANSPARENT( );
				// }
				fcPreview.setColor( cdCurrent );
				fcPreview.redraw( );
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	public void widgetDefaultSelected( SelectionEvent e )
	{
		if ( e.widget.equals( cmbFontSizes ) )
		{
			handleFontSize( );
		}
	}

	private void handleFontSize( )
	{
		if ( cmbFontSizes.getText( ).equals( ChartUIUtil.FONT_AUTO ) )
		{
			fdCurrent.unsetSize( );
		}
		else
		{
			boolean oldIsset = fdCurrent.isSetSize( );
			float oldValue = fdCurrent.getSize( );
			boolean isCorrect = true;
			int value = 0;
			try
			{
				value = Integer.valueOf( cmbFontSizes.getText( ) ).intValue( );
				if ( value <= 0 || value > 7200 )
				{
					isCorrect = false;
				}
			}
			catch ( NumberFormatException e )
			{
				isCorrect = false;
			}
			if ( !isCorrect )
			{
				cmbFontSizes.setText( oldIsset
						? String.valueOf( (int) oldValue )
						: ChartUIUtil.FONT_AUTO );
			}
			else
			{
				fdCurrent.setSize( value );
			}
		}
		updatePreview( );
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.birt.chart.ui.swt.composites.IAngleChangeListener#angleChanged(int)
	 */
	public void angleChanged( int iNewAngle )
	{
		iscRotation.setValue( iNewAngle );
		fdCurrent.setRotation( iNewAngle );
		updatePreview( );
	}

	public void focusGained( FocusEvent e )
	{
		// TODO Auto-generated method stub

	}

	public void focusLost( FocusEvent e )
	{
		if ( e.widget.equals( cmbFontSizes ) )
		{
			// Notify selectionListener to save values
			cmbFontSizes.notifyListeners( SWT.DefaultSelection, null );
		}

	}
	
	/**
	 * Validate whether the axes is flipped.
	 * 
	 * @return true if the chart orientation is horizontal.
	 */
	private boolean isFlippedAxes( )
	{
		return ( wizardContext.getModel( ) instanceof ChartWithAxes )
				&& ( ( (ChartWithAxes) wizardContext.getModel( ) ).getOrientation( )
						.getValue( ) == Orientation.HORIZONTAL );
	}
}